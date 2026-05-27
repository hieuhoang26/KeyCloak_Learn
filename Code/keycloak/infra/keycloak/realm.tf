resource "keycloak_realm" "backend-realm" {
  realm             = "kc-be"
  enabled           = true
  display_name      = "be-realm"
  display_name_html = "<b>be-realm</b>"

  # login setting
  registration_allowed = true
  ssl_required    = "external"

  # theme
  login_theme = "base"

  # token
  access_code_lifespan = "1h"

  # authentication settings
  password_policy = "upperCase(1) and length(8) and forceExpiredPasswordChange(365) and notUsername"
  attributes      = {
    mycustomAttribute = "myCustomValue"
  }

  // mail
  # smtp_server {
  #   host = "smtp.example.com"
  #   from = "example@example.com"
  #
  #   auth {
  #     username = "tom"
  #     password = "password"
  #   }
  # }

  security_defenses {
    headers {
      x_frame_options                     = "DENY"
      content_security_policy             = "frame-src 'self'; frame-ancestors 'self'; object-src 'none';"
      content_security_policy_report_only = ""
      x_content_type_options              = "nosniff"
      x_robots_tag                        = "none"
      x_xss_protection                    = "1; mode=block"
      strict_transport_security           = "max-age=31536000; includeSubDomains"
    }
    brute_force_detection {
      permanent_lockout                 = false
      max_login_failures                = 30
      wait_increment_seconds            = 60
      quick_login_check_milli_seconds   = 1000
      minimum_quick_login_wait_seconds  = 60
      max_failure_wait_seconds          = 900
      failure_reset_time_seconds        = 43200
    }
  }

  web_authn_policy {
    relying_party_entity_name = "Example"
    relying_party_id          = "keycloak.example.com"
    signature_algorithms      = ["ES256", "RS256"]
  }

  internationalization {
    supported_locales = [
      "en",
      "es"
    ]
    default_locale    = "en"
  }
}