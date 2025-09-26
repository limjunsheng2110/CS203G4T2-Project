export default function Home() {
  return (
    <section className="card">
      <h1>Welcome 👋</h1>
      <p>This is the frontend for <code>tariffg4t2</code>. Head to the Calculator to try the tariff computation against your Spring Boot API.</p>
      <p className="notice">Tip: Update the API base URL in <code>.env.development</code> or use the Vite dev proxy.</p>
    </section>
  )
}
