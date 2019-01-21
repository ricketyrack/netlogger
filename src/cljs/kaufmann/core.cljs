(ns kaufmann.core
    (:require [reagent.core     :as reagent :refer [adapt-react-class atom ]]
              [reagent.session  :as session]
              [cljsjs.react-bootstrap]
              [cljs.core.async              :refer [<!]]
              [cljs-http.client :as http]
              [reitit.frontend  :as reitit]
              [clerk.core       :as clerk]
              [accountant.core  :as accountant]))

;; -------------------------
;; Routes

(def router
  (reitit/router
   [["/"         :index]
    ["/services" :services]
    ["/contact"  :contact]
    ["/about"    :about]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

(defonce activeclass "active")
(defonce classes     (reagent/atom {:home      "nav-item active"
                                    :services  "nav-item"
                                    :contact   "nav-item"
                                    :about     "nav-item"}))

;; page level stuff
(defn on-js-reload []
  (println "Reloaded"))

;; utils
(defn clearclasses []
  (doseq [item @classes]
    (swap! classes assoc (key item) "nav-item")))

(defn clearandswap [k]
  (println "clearandswap " k)
  (clearclasses)
  (swap! classes assoc k activeclass))

(defn handlenavclick [event]
  "handle click somewhere in the nav bar"
  (println "navclick ")
  (case event.target.id
    "home"       (clerk/navigate-page! (path-for :index))
    ;; "" (+ 1 1)  (clerk/navigate-page! (path-for :index))
    "services"   (clerk/navigate-page! (path-for :services))
     "about"     (clerk/navigate-page! (path-for :about))
     "contact"   (clerk/navigate-page! (path-for :contact))
     "default")
  (clearandswap (if (= event.target.id "home") :home (keyword event.target.id)))
  (.preventDefault event)
  nil)

;; macro
(defmacro def-reagent-class [var-name js-ns js-name]
  `(def ~var-name
     (reagent/adapt-react-class
      (aget ~js-ns ~js-name))))

;; (path-for :index)

;; -------------------------
;; Page components

(defn navitem [id myclass mypath label & [icon]]
      [:li {:class myclass}
       [:a { :href mypath
            :id id :class "nav-link"}
        (if icon
          [icon {:aria-hidden "false"}])
        (str " " label) [:span {:class "sr-on"}]]])

(def navbar
  [:nav {:class "navbar navbar-expand-md navbar-light bg-light"}
   [:a.navbar-brand {:href (path-for :index)} "Kaufmann"]
   [:button {:class "navbar-toggler" :type "button" :data-toggle "collapse"
             :data-target "#navbarSupportedContent" :aria-controls "navbarSupportedContent"
             :aria-expanded "false" :aria-label "Toggle navigation"}
    [:span {:class "navbar-toggler-icon"}]]
    ;; collect the nav links, forms, and other content toggling
    [:div {:class "collapse navbar-collapse" :id "navbarSupportedContent"}
     [:ul {:class "navbar-nav mr-auto"}
      (navitem "home" (@classes :index)
               (path-for :index)
               "Home"     :i.fa.fa-home)
      (navitem "services" (@classes :services)
               (path-for  :services)
               "Services" :i.fa.fa-wrench)
      (navitem "contact" (@classes :contact)
               (path-for :contact)
               "Contact" :i.fa.fa-phone)
      (navitem "about" (@classes :about)
               (path-for :about)
               "About" :i.fa.fa-info)]
     [:ul {:class "nav navbar-nav navbar-right"}]]])

(defn home-page []
  (fn []
    [:span.main
     [:p.home "Only choose the best when it comes to your money!"]
     [:p.home2 "For over 28 years Rodney Kaufmann has specialized in providing individuals
                and businesses throughout the USA with accounting and tax services."]]))

(defn contact-page []
  (fn []
    [:span.main
     [:h3 "Contacts"]
     [:div
      [:p.contact "RODNEY D. KAUFMANN CPA, INC."
       [:br]
       "5416 S YALE AVE"
       [:br]
       "SUITE 650"
       [:br]
       "TULSA, OK 74135"]
      [:p "918-747-7433"
       [:br]
       "918-747-7488 (Fax)"]
      [:a {:href "mailto:rod@kaufmanncpa.com"} "rod@kaufmanncpa.com"]
      [:p "M-F : 8am to 5pm"]]]))

(defn about-page []
  (fn []
    [:span.main
     [:h3 "About Kaufmann"]
     [:p "Test about paragraph"]]))

(defn services-page []
  (fn []
    [:span.main
     [:h3 "Kaufmann Services"]
     [:p "Rodney D. Kaufmann CPA provides a wide range of services to individuals and businesses in a variety of industries. With over 28 years of experience, we provide personalized service to meet each client’s specific needs in planning for the future and achieving their goals in an ever-changing financial and regulatory environment."

      [:ul
       [:li "Individual Tax Returns"]
       [:li "Corporate Tax Returns"]
       [:li "Partnership Tax Returns"]
       [:li "Not for Profit Tax Returns"]
       [:li "Tax Planning & Preparation"]
       [:li "IRS Representation"]
       [:li "Financial Statement Preparation"]
       [:li "Accounting Review"]
       [:li "Tax Audit Representation"]
       [:li "Estate & Trust Tax Preparation"]
       [:li "Sales Tax Compliance"]
       [:li "Reviews & Compilations"]
       [:li "Bookkeeping & Payroll"]
       [:li "Other Services"]]]]))

;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :index    #'home-page
    :services #'services-page
    :about    #'about-page
    :contact  #'contact-page))

;; -------------------------
;; Page mounting component

(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      [:div.container-fluid
       [:header.pad-head
        navbar]
       [page]
       [:footer.footer
        [:p "©2019 Rodney D. Kaufmann CPA, Inc."
         ]]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page]
                  (.getElementById js/document "app")))

(defn init! []
  (clerk/initialize!)
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (println path)

      (let [match (reitit/match-by-path router path)
            current-page (:name (:data  match))
            route-params (:path-params match)
            pathname     (second (re-matches #"^.(.*)" path))]
        (reagent/after-render clerk/after-render!)
        (session/put! :route {:current-page (page-for current-page)
                              :route-params route-params})
        (clerk/navigate-page! path)
        (clearandswap (if (= pathname "/") :home (keyword pathname)))
        ))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (mount-root))
