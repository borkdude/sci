(ns sci.impl.functions
  {:no-doc true}
  (:require [clojure.string :as str]
            [clojure.set :as set]
            #?(:clj [sci.impl.graal_1610])))

(def host-specific-functions
  #?(:cljs {}
     :clj {'+' +'
           '-' -'
           '*' *'
           'boolean-array boolean-array
           'bound? bound?
           'byte-array byte-array
           'bigint bigint
           'bytes? bytes?
           'biginteger biginteger
           'bigdec bigdec
           'char-array char-array
           'char-escape-string char-escape-string
           'char-name-string char-name-string
           'class class
           'dec' dec'
           'decimal? decimal?
           'denominator denominator
           'format format
           'float-array float-array
           'inc' inc'
           'line-seq line-seq
           'num num
           'namespace-munge namespace-munge
           'numerator numerator
           'replicate replicate
           'rational? rational?
           'ratio? ratio?
           'rationalize rationalize
           'seque seque
           'str/re-quote-replacement str/re-quote-replacement
           'xml-seq xml-seq}))

(def functions
  (merge {'= =
          '< <
          '<= <=
          '> >
          '>= >=
          '+ +
          '- -
          '* *
          '/ /
          '== ==
          'aget aget
          'alength alength
          'apply apply
          'assoc assoc
          'assoc-in assoc-in
          'associative? associative?
          'array-map array-map
          'bit-and-not bit-and-not
          'bit-set bit-set
          'bit-shift-left bit-shift-left
          'bit-shift-right bit-shift-right
          'bit-xor bit-xor
          'boolean boolean
          'boolean? boolean?
          'booleans booleans
          'butlast butlast
          'bytes bytes
          'bit-test bit-test
          'bit-and bit-and
          'bounded-count bounded-count
          'bit-or bit-or
          'bit-flip bit-flip
          'bit-not bit-not
          'byte byte
          'cat cat
          'char char
          'char? char?
          'conj conj
          'cons cons
          'contains? contains?
          'count count
          'cycle cycle
          'comp comp
          'concat concat
          'comparator comparator
          'coll? coll?
          'compare compare
          'complement complement
          'constantly constantly
          'chars chars
          'completing completing
          'counted? counted?
          'chunk-rest chunk-rest
          'chunk-next chunk-next
          'dec dec
          'dedupe dedupe
          'dissoc dissoc
          'distinct distinct
          'distinct? distinct?
          'disj disj
          'double double
          'double? double?
          'drop drop
          'drop-last drop-last
          'drop-while drop-while
          'doubles doubles
          'eduction eduction
          'empty empty
          'empty? empty?
          'even? even?
          'every? every?
          'every-pred every-pred
          'ensure-reduced ensure-reduced
          'first first
          'float? float?
          'floats floats
          'fnil fnil
          'fnext fnext
          'ffirst ffirst
          'flatten flatten
          'false? false?
          'filter filter
          'filterv filterv
          'find find
          'frequencies frequencies
          'float float
          'get get
          'get-in get-in
          'group-by group-by
          'gensym gensym
          'hash hash
          'hash-map hash-map
          'hash-set hash-set
          'hash-unordered-coll hash-unordered-coll
          'ident? ident?
          'identical? identical?
          'identity identity
          'inc inc
          'int-array int-array
          'interleave interleave
          'into into
          'iterate iterate
          'int int
          'int? int?
          'interpose interpose
          'indexed? indexed?
          'integer? integer?
          'ints ints
          'into-array into-array
          'juxt juxt
          'keep keep
          'keep-indexed keep-indexed
          'key key
          'keys keys
          'keyword keyword
          'keyword? keyword?
          'last last
          'long long
          'list list
          'list? list?
          'longs longs
          'list* list*
          'long-array long-array
          'map map
          'map? map?
          'map-indexed map-indexed
          'map-entry? map-entry?
          'mapv mapv
          'mapcat mapcat
          'max max
          'max-key max-key
          'meta meta
          'merge merge
          'merge-with merge-with
          'min min
          'min-key min-key
          'munge munge
          'mod mod
          'make-array make-array
          'name name
          'newline newline
          'nfirst nfirst
          'not not
          'not= not=
          'not-every? not-every?
          'neg? neg?
          'neg-int? neg-int?
          'nth nth
          'nthnext nthnext
          'nthrest nthrest
          'nil? nil?
          'nat-int? nat-int?
          'number? number?
          'not-empty not-empty
          'not-any? not-any?
          'next next
          'nnext nnext
          'odd? odd?
          'object-array object-array
          'peek peek
          'pop pop
          'pos? pos?
          'pos-int? pos-int?
          'partial partial
          'partition partition
          'partition-all partition-all
          'partition-by partition-by
          'qualified-ident? qualified-ident?
          'qualified-symbol? qualified-symbol?
          'qualified-keyword? qualified-keyword?
          'quot quot
          're-seq re-seq
          're-find re-find
          're-pattern re-pattern
          're-matches re-matches
          'rem rem
          'remove remove
          'rest rest
          'repeatedly repeatedly
          'reverse reverse
          'rand-int rand-int
          'rand-nth rand-nth
          'range range
          'reduce reduce
          'reduce-kv reduce-kv
          'reduced reduced
          'reduced? reduced?
          'reversible? reversible?
          'rsubseq rsubseq
          'reductions reductions
          'rand rand
          'replace replace
          'rseq rseq
          'random-sample random-sample
          'repeat repeat
          'set? set?
          'sequential? sequential?
          'select-keys select-keys
          'simple-keyword? simple-keyword?
          'simple-symbol? simple-symbol?
          'some? some?
          'string? string?
          'str str
          'set/difference set/difference
          'set/index set/index
          'set/intersection set/intersection
          'set/join set/join
          'set/map-invert set/map-invert
          'set/project set/project
          'set/rename set/rename
          'set/rename-keys set/rename-keys
          'set/select set/select
          'set/subset? set/subset?
          'set/superset? set/superset?
          'set/union set/union
          'str/blank? str/blank?
          'str/capitalize str/capitalize
          'str/ends-with? str/ends-with?
          'str/escape str/escape
          'str/includes? str/includes?
          'str/index-of str/index-of
          'str/join str/join
          'str/last-index-of str/last-index-of
          'str/lower-case str/lower-case
          'str/replace str/replace
          'str/replace-first str/replace-first
          'str/reverse str/reverse
          'str/split str/split
          'str/split-lines str/split-lines
          'str/starts-with? str/starts-with?
          'str/trim str/trim
          'str/trim-newline str/trim-newline
          'str/triml str/triml
          'str/trimr str/trimr
          'str/upper-case str/upper-case
          'second second
          'set set
          'seq seq
          'seq? seq?
          'short short
          'shuffle shuffle
          'sort sort
          'sort-by sort-by
          'subs subs
          'symbol symbol
          'symbol? symbol?
          'special-symbol? special-symbol?
          'subvec subvec
          'some-fn some-fn
          'some some
          'split-at split-at
          'split-with split-with
          'sorted-set sorted-set
          'subseq subseq
          'sorted-set-by sorted-set-by
          'sorted-map-by sorted-map-by
          'sorted-map sorted-map
          'sorted? sorted?
          'simple-ident? simple-ident?
          'sequence sequence
          'seqable? seqable?
          'shorts shorts
          'take take
          'take-last take-last
          'take-nth take-nth
          'take-while take-while
          'transduce transduce
          'tree-seq tree-seq
          'type type
          'true? true?
          'to-array to-array
          'update update
          'update-in update-in
          'uri? uri?
          'uuid? uuid?
          'unchecked-inc-int unchecked-inc-int
          'unchecked-long unchecked-long
          'unchecked-negate unchecked-negate
          'unchecked-remainder-int unchecked-remainder-int
          'unchecked-subtract-int unchecked-subtract-int
          'unsigned-bit-shift-right unsigned-bit-shift-right
          'unchecked-float unchecked-float
          'unchecked-add-int unchecked-add-int
          'unchecked-double unchecked-double
          'unchecked-multiply-int unchecked-multiply-int
          'unchecked-int unchecked-int
          'unchecked-multiply unchecked-multiply
          'unchecked-dec-int unchecked-dec-int
          'unchecked-add unchecked-add
          'unreduced unreduced
          'unchecked-divide-int unchecked-divide-int
          'unchecked-subtract unchecked-subtract
          'unchecked-negate-int unchecked-negate-int
          'unchecked-inc unchecked-inc
          'unchecked-char unchecked-char
          'unchecked-byte unchecked-byte
          'unchecked-short unchecked-short
          'val val
          'vals vals
          'vary-meta vary-meta
          'vec vec
          'vector vector
          'vector? vector?
          'zipmap zipmap
          'zero? zero?}
         host-specific-functions))
