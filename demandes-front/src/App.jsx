import { useEffect, useState } from 'react'
import keycloak from './keycloak'
import { appelerApi } from './api'

export default function App() {
  const username = keycloak.tokenParsed?.preferred_username
  const roles = keycloak.tokenParsed?.realm_access?.roles ?? []
  const estAdmin = roles.includes('ADMIN')

  const [objet, setObjet] = useState('')
  const [mesDemandes, setMesDemandes] = useState([])
  const [toutes, setToutes] = useState([])
  const [profil, setProfil] = useState(null)
  const [erreur, setErreur] = useState('')

  async function executer(action) {
    setErreur('')
    try {
      await action()
    } catch (e) {
      setErreur(e.message)
    }
  }

  const chargerMesDemandes = () =>
    executer(async () => setMesDemandes(await appelerApi('/api/demandes')))

  const chargerToutes = () =>
    executer(async () => setToutes(await appelerApi('/api/admin/demandes')))

  const chargerProfil = () =>
    executer(async () => setProfil(await appelerApi('/api/profil')))

  const creer = (e) => {
    e.preventDefault()
    executer(async () => {
      await appelerApi('/api/demandes', {
        method: 'POST',
        body: JSON.stringify({ objet }),
      })
      setObjet('')
      setMesDemandes(await appelerApi('/api/demandes'))
    })
  }

  const changerStatut = (id, statut) =>
    executer(async () => {
      await appelerApi('/api/admin/demandes/' + id, {
        method: 'PATCH',
        body: JSON.stringify({ statut }),
      })
      setToutes(await appelerApi('/api/admin/demandes'))
    })

  useEffect(() => {
    chargerMesDemandes()
    if (estAdmin) chargerToutes()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div className="page">
      <header>
        <div>
          <h1>Gestion des demandes</h1>
          <p>
            Connecte : <b>{username}</b>{' '}
            {roles
              .filter((r) => r === 'USER' || r === 'ADMIN')
              .map((r) => (
                <span key={r} className="badge">
                  {r}
                </span>
              ))}
          </p>
        </div>
        <button
          className="secondaire"
          onClick={() => keycloak.logout({ redirectUri: 'http://localhost:5173/' })}
        >
          Deconnexion
        </button>
      </header>

      {erreur && <p className="erreur">{erreur}</p>}

      <section>
        <h2>Nouvelle demande</h2>
        <form onSubmit={creer}>
          <input
            value={objet}
            onChange={(e) => setObjet(e.target.value)}
            placeholder="Objet de la demande"
          />
          <button type="submit">Envoyer</button>
        </form>
      </section>

      <section>
        <h2>Mes demandes</h2>
        <Tableau demandes={mesDemandes} />
      </section>

      <section>
        <h2>Mon profil (GET /api/profil)</h2>
        <button className="secondaire" onClick={chargerProfil}>
          Afficher mon profil
        </button>
        {profil && <pre>{JSON.stringify(profil, null, 2)}</pre>}
      </section>

      {estAdmin && (
        <section>
          <h2>Administration : toutes les demandes</h2>
          <Tableau
            demandes={toutes}
            avecProprietaire
            actions={(d) => (
              <>
                <button
                  disabled={d.statut === 'VALIDEE'}
                  onClick={() => changerStatut(d.id, 'VALIDEE')}
                >
                  Valider
                </button>{' '}
                <button
                  className="danger"
                  disabled={d.statut === 'REFUSEE'}
                  onClick={() => changerStatut(d.id, 'REFUSEE')}
                >
                  Refuser
                </button>
              </>
            )}
          />
        </section>
      )}
    </div>
  )
}

function Tableau({ demandes, avecProprietaire = false, actions }) {
  if (demandes.length === 0) return <p className="vide">Aucune demande.</p>

  return (
    <table>
      <thead>
        <tr>
          <th>ID</th>
          {avecProprietaire && <th>Proprietaire (sub)</th>}
          <th>Objet</th>
          <th>Statut</th>
          {actions && <th>Actions</th>}
        </tr>
      </thead>
      <tbody>
        {demandes.map((d) => (
          <tr key={d.id}>
            <td>{d.id}</td>
            {avecProprietaire && <td className="mono">{d.owner.slice(0, 8)}…</td>}
            <td>{d.objet}</td>
            <td>
              <span className={'statut ' + d.statut}>{d.statut}</span>
            </td>
            {actions && <td>{actions(d)}</td>}
          </tr>
        ))}
      </tbody>
    </table>
  )
}
