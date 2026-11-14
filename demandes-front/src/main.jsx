import React from 'react'
import ReactDOM from 'react-dom/client'
import keycloak from './keycloak'
import App from './App'
import './App.css'

const root = ReactDOM.createRoot(document.getElementById('root'))

// Authorization Code Flow + PKCE : redirection vers Keycloak si non connecte.
keycloak
  .init({
    onLoad: 'login-required',
    pkceMethod: 'S256',
    checkLoginIframe: false,
  })
  .then((authenticated) => {
    if (authenticated) {
      root.render(
        <React.StrictMode>
          <App />
        </React.StrictMode>
      )
    } else {
      keycloak.login()
    }
  })
  .catch((e) => {
    console.error('Echec init Keycloak', e)
    root.render(
      <p style={{ padding: 24 }}>
        Impossible de contacter Keycloak (http://localhost:8080). Verifiez que le
        conteneur est demarre et que le realm <b>enset-realm</b> existe.
      </p>
    )
  })
