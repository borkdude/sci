(ns sci.impl.io
  {:no-doc true}
  (:refer-clojure :exclude [pr prn print println newline flush
                            with-out-str with-in-str read-line
                            #?@(:cljs [string-print])])
  (:require [sci.impl.vars :as vars]
            #?(:clj [sci.impl.io :as sio])
            #?(:cljs [goog.string])))

#?(:clj (set! *warn-on-reflection* true))

(def init-in (fn []
               #?(:clj (-> (java.io.StringReader. "")
                           clojure.lang.LineNumberingPushbackReader.))))

(def init-out (fn [] (new #?(:clj java.io.StringWriter
                             :cljs goog.string/StringBuffer))))

(def init-err (fn [] #?(:clj (java.io.StringWriter.))))

(def in (vars/dynamic-var '*in* (init-in)))

(def out (vars/dynamic-var '*out* (init-out)))

(def err (vars/dynamic-var '*err* (init-err)))

#?(:clj (defn pr-on
          {:private true
           :static true}
          [x w]
          (if *print-dup*
            (print-dup x w)
            (print-method x w))
          nil))

#?(:clj (defn pr
          ([] nil)
          ([x]
           (pr-on x @out))
          ([x & more]
           (pr x)
           (. ^java.io.Writer @out (append \space))
           (if-let [nmore (next more)]
             (recur (first more) nmore)
             (apply pr more))))
   :cljs (defn pr
           [& objs]
           (.append @out (apply pr-str objs))))

#?(:clj
   (defn flush
     []
     (. ^java.io.Writer @out (flush))
     nil)
   :cljs (defn flush [] ;stub
           nil))

#?(:cljs (declare println))

#?(:clj (defn newline
          []
          (. ^java.io.Writer @out (append ^String @#'clojure.core/system-newline))
          nil)
   :cljs (defn newline
           []
           (println)))

#?(:clj
   (defn prn
     [& more]
     (apply pr more)
     (newline)
     (when *flush-on-newline*
       (flush)))
   :cljs
   (defn prn
     [& objs]
     (.append @out (apply prn-str objs))))

#?(:clj
   (defn print
     [& more]
     (binding [*print-readably* nil]
       (apply pr more)))
   :cljs
   (defn print
     [& objs]
     (.append @out (apply print-str objs))))

#?(:clj
   (defn println
     [& more]
     (binding [*print-readably* nil]
       (apply prn more)))
   :cljs
   (defn println
     [& objs]
     (.append @out (apply println-str objs))))

(defn with-out-str
  [_ _ & body]
  `(let [s# (new #?(:clj java.io.StringWriter
                    :cljs goog.string.StringBuffer))]
     (binding [*out* s#]
       ~@body
       (str s#))))

#?(:clj
   (defn with-in-str
     [_ _ s & body]
     `(with-open [s# (-> (java.io.StringReader. ~s) clojure.lang.LineNumberingPushbackReader.)]
        (binding [*in* s#]
          ~@body))))

#?(:clj
   (defn read-line
     []
     (if (instance? clojure.lang.LineNumberingPushbackReader @in)
       (.readLine ^clojure.lang.LineNumberingPushbackReader @in)
       (.readLine ^java.io.BufferedReader @in))))

;; #?(:clj
;;    (defmacro with-sci-out-str
;;      "For external use. Useful for testing sci programs."
;;      [& body]
;;      `(let [sw# (java.io.StringWriter.)
;;             _# (try (vars/push-thread-bindings {sio/out sw#})
;;                     (do ~@body)
;;                     (finally (vars/pop-thread-bindings)))
;;             out# (str sw#)]
;;         out#)))

#?(:clj
   (defmacro with-sci-in-str
     "For external use. Useful for testing sci programs."
     [s & body]
     `(with-open [s# (-> (java.io.StringReader. ~s) clojure.lang.LineNumberingPushbackReader.)]
        (try (vars/push-thread-bindings {sio/in s#})
             (do ~@body)
             (finally (vars/pop-thread-bindings))))))
