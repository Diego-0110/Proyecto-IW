Feature: login en servidor

  
  Scenario: login correcto como b
    Given driver baseUrl + '/login'
    And input('#username', 'igoford0')
    And input('#password', 'aa')
    When submit().click(".form-signin button")
    Then waitForUrl(baseUrl + '/user/3')
    When click("#add-event")
    Then waitForUrl(baseUrl + '/formAddEvent')
    When input("#title", "ESPAÑA")
    Then input("#initDate", "11/06/2023")
    And input("#finishDate", "15/06/2023")
    And input("#destination", "Madrid")
    And input("#reunionPoint", "Madrid")
    And input("#description", "This event...")
    And input("#price", "10")
    And input("#capacity", "5")
    And input("#transport", "BUS")
    And input("#notes", "NO_KIDS")
    And input("#type", "CAMPING")
    When click("#add-event")
    Then delay(1000)
    And click("#delete-event")