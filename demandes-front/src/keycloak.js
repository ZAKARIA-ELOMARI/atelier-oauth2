import Keycloak from 'keycloak-js'

// Client public : aucun secret dans le navigateur, la securite repose sur PKCE.
const keycloak = new Keycloak({
  url: 'http://localhost:8080',
  realm: 'enset-realm',
  clientId: 'react-client',
})

export default keycloak
