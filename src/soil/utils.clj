(ns soil.utils)

(defn assoc-in-if
  [m ks v]
  (if (some? v)
    (assoc-in m ks v)
    m))

(defn assoc-if [m k v] (assoc-in-if m [k] v))

(defn update-in-if
  [m ks f]
  (if-let [old-val (get-in m ks)]
    (assoc-in-if m ks (f old-val))
    m))

(defn update-if [m k f] (update-in-if m [k] f))

(defn find-first [s pred] (first (filter pred s)))
