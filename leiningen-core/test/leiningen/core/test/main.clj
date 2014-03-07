(ns leiningen.core.test.main
  (:use [clojure.test]
        [leiningen.core.main]))

(deftest test-task-args-help-pass-through
  (let [task-name "sirius"
        alias [task-name]
        help-res ["help" alias]
        alias-1 "dog"
        alias-2 "uncle"
        project {:aliases {alias-1 alias
                           alias-2 task-name}}]

    (testing "with :pass-through-help meta"
      (testing "on a var"
        (are [res arg] (= res (task-args arg project))
             help-res ["help" task-name]
             [task-name ["-h"]] [task-name "-h"]
             [task-name ["-?"]] [task-name "-?"]
             [task-name ["--help"]] [task-name "--help"]
             [task-name []] [task-name]))
      (testing "on an alias"
        (are [res arg] (= res (task-args arg project))
             ["help" [alias-1]] ["help" alias-1]
             ["help" [alias-2]] ["help" alias-2]
             [task-name ["-h"]] [alias-2 "-h"]
             [[task-name] ["-?"]] [alias-1 "-?"]
             [task-name ["--help"]] [alias-2 "--help"]
             [[task-name] []] [alias-1]
             [task-name []] [alias-2])))))

(deftest test-matching-arity
  (is (not (matching-arity? (resolve-task "bluuugh") ["bogus" "arg" "s"])))
  (is (matching-arity? (resolve-task "bluuugh") []))
  (is (matching-arity? (resolve-task "var-args") []))
  (is (matching-arity? (resolve-task "var-args") ["test-core" "hey"]))
  (is (not (matching-arity? (resolve-task "one-or-two") [])))
  (is (matching-arity? (resolve-task "one-or-two") ["clojure"]))
  (is (matching-arity? (resolve-task "one-or-two") ["clojure" "2"]))
  (is (not (matching-arity? (resolve-task "one-or-two") ["clojure" "2" "3"]))))

(deftest test-version-satisfies
  (is (version-satisfies? "1.5.0" "1.4.2"))
  (is (not (version-satisfies? "1.4.2" "1.5.0")))
  (is (version-satisfies? "1.2.3" "1.1.1"))
  (is (version-satisfies? "1.2.0" "1.2"))
  (is (version-satisfies? "1.2" "1"))
  (is (not (version-satisfies? "1.67" "16.7"))))

(deftest one-or-two-args
  (try (binding [*err* (java.io.StringWriter.)]
         (resolve-and-apply {:root true} ["one-or-two"]))
       (catch clojure.lang.ExceptionInfo e
         (re-find #"(?s)Wrong number of arguments to one-or-two task.*Expected \[one\] or \[one two\]"
                  (.getMessage e)))))

(deftest zero-args-msg
  (try (binding [*err* (java.io.StringWriter.)]
         (resolve-and-apply {:root true} ["zero" "too" "many" "args"]))
       (catch clojure.lang.ExceptionInfo e
         (re-find #"(?s)Wrong number of arguments to zero task.*Expected \[\]"
                  (.getMessage e)))))

(def ^:private distance @#'leiningen.core.main/distance)

(deftest test-damerau-levensthein
  (is (zero? (distance "run" "run")))
  (is (zero? (distance "uberjar" "uberjar")))
  (is (zero? (distance "classpath" "classpath")))
  (is (zero? (distance "with-profile" "with-profile")))

  (is (= 1 (distance "rep" "repl")))
  (is (= 1 (distance "est" "test")))
  (is (= 1 (distance "java" "javac")))
  (is (= 1 (distance "halp" "help")))
  (is (= 1 (distance "lien" "lein")))

  (is (= 4 (distance "" "repl")))
  (is (= 6 (distance "foobar" "")))

  (is (= 2 (distance "erlp" "repl")))
  (is (= 2 (distance "deploy" "epdloy")))
  (is (= 3 (distance "pugared" "upgrade"))))

(deftest test-parse-options
  (is (= (parse-options ["--chicken"])
         [{:--chicken true} '()]))

  (is (= (parse-options ["--beef" "rare"])
         [{:--beef "rare"} []]))

  (is (= (parse-options [":fish" "salmon"])
         [{:fish "salmon"} []]))

  (is (= (parse-options ["salmon" "trout"])
         [{} ["salmon" "trout"]]))

  (is (= (parse-options ["--to-dir" "test2" "--ham"])
         [{:--ham true, :--to-dir "test2"} []]))

  (is (= (parse-options ["--to-dir" "test2" "--ham" "--" "pate"])
         [{:--ham true, :--to-dir "test2"} ["pate"]]))

  (is (= (parse-options ["--ham" "--to-dir" "test2" "pate"])
         [{:--ham true, :--to-dir "test2"} ["pate"]]))

  (is (= (parse-options ["--to-dir" "test2" "--ham" "--"])
         [{:--ham true, :--to-dir "test2"} []])))
