(ns sci.impl.macros
  (:refer-clojure :exclude [destructure macroexpand macroexpand-all macroexpand-1])
  (:require [clojure.walk :refer [prewalk postwalk]]
            [sci.impl.destructure :refer [destructure]]
            [sci.impl.functions :as f]
            [clojure.string :as str]))

(def macros '#{do if when and or -> ->> as-> quote let fn def defn})

(defn lookup [{:keys [:env :bindings]} sym]
  (when-let [[k v :as kv]
             (or (when-let [v (get macros sym)]
                   [v v])
                 (find bindings sym)
                 (find @env sym)
                 (find f/functions sym))]
    (if-let [m (meta k)]
      (if (:sci/deref! m)
        ;; the evaluation of this expression has been delayed by
        ;; the caller and now is the time to deref it
        [k @v] kv)
      kv)))

(defn resolve-symbol [ctx expr]
  ;; (prn "resolve sym" expr)
  (second
   (or
    (lookup ctx expr)
    ;; TODO: check if symbol is in macros and then emit an error: cannot take
    ;; the value of a macro
    (let [n (name expr)]
      (if (str/starts-with? n "'")
        (let [v (symbol (subs n 1))]
          [v v])
        (throw (new #?(:clj Exception
                       :cljs js/Error)
                    (str "Could not resolve symbol: " (str expr)
                         (keys (:bindings expr))))))))))

(declare macroexpand macroexpand-1)

(defn expand-fn-args+body [ctx [binding-vector & body-exprs]]
  ;; (prn "body exprs" body-exprs)
  (let [fixed-args (take-while #(not= '& %) binding-vector)
        var-arg (second (drop-while #(not= '& %) binding-vector))
        fixed-arity (count fixed-args)
        fixed-names (vec (repeatedly fixed-arity gensym))
        destructure-vec (vec (interleave binding-vector fixed-names))
        var-arg-name (when var-arg (gensym))
        destructure-vec (if var-arg
                          (conj destructure-vec var-arg var-arg-name)
                          destructure-vec)
        arg-bindings (apply hash-map (interleave fixed-names (repeat nil)))
        ctx (cond-> (update ctx :bindings merge arg-bindings)
              var-arg
              (assoc-in [:bindings var-arg-name] nil))
        destructured-vec (destructure destructure-vec)
        ctx (update ctx :bindings merge (zipmap (take-nth 2 destructured-vec)
                                                (repeat nil)))
        body-form (with-meta `(~'let ~destructured-vec
                               ~@(doall (map #(macroexpand ctx %) body-exprs)))
                    {:sci/expanded true})
        arg-list (if var-arg
                   (conj fixed-names '& var-arg-name)
                   fixed-names)]
    (list arg-list body-form)))

(defn expand-fn [ctx [_fn name? & body]]
  (let [fn-name (if (symbol? name?)
                  name?
                  nil)
        body (if fn-name
               body
               (cons name? body))
        fn-name (or fn-name (gensym "fn"))
        bodies (if (seq? (first body))
                 body
                 [body])
        ctx (assoc-in ctx [:bindings fn-name] nil)
        arities (doall (map #(expand-fn-args+body ctx %) bodies))
        form (list* 'fn fn-name arities)]
    form))

(defn expand-fn-literal [ctx expr]
  (let [state (volatile! {:max-fixed 0 :var-args? false})
        expr (postwalk (fn [elt]
                         (if (symbol? elt)
                           (if-let [[_ m] (re-matches #"^%(.*)" (name elt))]
                             (cond (empty? m)
                                   (do (vswap! state update :max-fixed max 1)
                                       '%1)
                                   (= "&" m)
                                   (do (vswap! state assoc :var-args? true)
                                       elt)
                                   :else (do (let [n #?(:clj (Integer/parseInt m)
                                                        :cljs (js/parseInt m))]
                                               (vswap! state update :max-fixed max n))
                                             elt))
                             elt)
                           elt))
                       expr)
        {:keys [:max-fixed :var-args?]} @state
        fixed-names (map #(symbol (str "%" %)) (range 1 (inc max-fixed)))
        arg-list (vec (concat fixed-names (when var-args?
                                            ['& '%&])))
        ctx (update ctx :bindings merge (zipmap fixed-names (repeat nil)))
        ctx (if var-args?
              (update ctx :bindings assoc '%& nil)
              ctx)
        form (with-meta (list 'fn (list arg-list (macroexpand ctx expr)))
               {:sci/expanded true})]
    form
    #_(expand-fn ctx form)))

(defn expand-let*
  [ctx destructured-let-bindings exprs]
  (let [[ctx new-let-bindings]
        (reduce
         (fn [[ctx new-let-bindings] [binding-name binding-value]]
           (let [v (macroexpand ctx binding-value)]
             [(update ctx :bindings assoc binding-name v)
              (conj new-let-bindings binding-name v)]))
         [ctx []]
         (partition 2 destructured-let-bindings))]
    (with-meta `(~'let ~new-let-bindings ~@(doall (map #(macroexpand ctx %) exprs)))
      {:sci/expanded true})))

(defn expand-let
  "The let macro from clojure.core"
  [ctx [_let let-bindings  & exprs]]
  (let [let-bindings (destructure let-bindings)]
    (expand-let* ctx let-bindings exprs)))

(defn expand-as->
  "The ->> macro from clojure.core."
  [ctx [_as expr name & forms]]
  `(~'let [~name ~expr
         ~@(interleave (repeat name) (butlast forms))]
     ~(if (empty? forms)
        name
        (last forms))))

(defn expand-def
  [ctx [_def var-name ?docstring ?init]]
  (let [docstring (when ?init ?docstring)
        init (if docstring ?init ?docstring)
        init (macroexpand ctx init)
        m (if docstring {:sci/doc docstring} {})
        var-name (with-meta var-name m)]
    (swap! (:env ctx) assoc var-name :sci/var.unbound)
    (list 'def var-name init)))


(defn expand-defn [ctx [_defn fn-name docstring? & body]]
  (let [docstring (when (string? docstring?) docstring?)
        body (if docstring body (cons docstring? body))
        fn-body (list* 'fn fn-name body)
        f (expand-fn ctx fn-body)]
    (swap! (:env ctx) assoc fn-name :sci/var.unbound)
    (list 'def fn-name f)))

(comment

  (expand-as-> nil '(as-> 1 x (inc x) (inc x)))
  )

(def dbg (atom []))

(defn macroexpand
  [ctx expr]
  (let [m (meta expr)
        res (if (:sci/expanded m) expr
                (cond
                  (symbol? expr)
                  (or (let [v (resolve-symbol ctx expr)]
                        (when-not (identical? :sci/var.unbound v)))
                      expr)
                  (seq? expr)
                  (if-let [f (first expr)]
                    (let [f (get macros f)]
                      (case f
                        let
                        (expand-let ctx expr)
                        fn (expand-fn ctx expr)
                        quote expr
                        def (expand-def ctx expr)
                        defn (expand-defn ctx expr)
                        as-> (expand-as-> ctx expr)
                        ;; else:
                        (doall (map #(macroexpand ctx %) expr))))
                    expr)
                  :else expr))]
    ;; (prn expr '-> res)
    res))

;;;; Scratch

(comment
  (macroexpand {:bindings f/functions} '(+ 1 2 3))
  (macroexpand {:bindings f/functions} '(fn [x] y))
  (macroexpand {:env (atom {}) :bindings f/functions} '(defn f [x] x))
  (macroexpand {:env (atom {}) :bindings f/functions} '((fn foo [x] (if (< x 3) (foo 1 (inc x)) x)) 0))
  @dbg

  )
