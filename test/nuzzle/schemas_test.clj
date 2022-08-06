(ns nuzzle.schemas-test)

;; (deftest syntax-highlighter
;;   (is (m/validate schemas/syntax-highlighter
;;                   {:provider :chroma
;;                    :style "emacs"})))
;;
;; (deftest local-date
;;   (is (m/validate schemas/local-date
;;                   (m/decode schemas/local-date "2022-05-04"
;;                             (mt/transformer {:name :local-date}))))
;;   (is (not (m/validate schemas/local-date
;;                        (m/decode schemas/local-date "2022-05-04jhfklsdjf"
;;                                  (mt/transformer {:name :local-date}))))))
