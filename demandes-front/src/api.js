import keycloak from './keycloak'

const API = 'http://localhost:8081'

/**
 * Appelle l'API avec l'Access Token. Le jeton est renouvele s'il expire
 * dans moins de 30 secondes (refresh token gere par keycloak-js).
 */
export async function appelerApi(chemin, options = {}) {
  try {
    await keycloak.updateToken(30)
  } catch {
    keycloak.login()
    throw new Error('Session expiree')
  }

  const reponse = await fetch(API + chemin, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: 'Bearer ' + keycloak.token,
      ...(options.headers || {}),
    },
  })

  if (!reponse.ok) {
    let message = 'Erreur ' + reponse.status
    try {
      const corps = await reponse.json()
      if (corps.message) message += ' : ' + corps.message
    } catch {
      /* corps vide (401/403) */
    }
    throw new Error(message)
  }
  return reponse.json()
}
