import { Link, Route, Routes, NavLink } from 'react-router-dom'
import Home from './pages/Home'
import Calculator from './pages/Calculator'

export default function App() {
  return (
    <div className="app-shell">
      <header className="topbar">
        <Link to="/" className="brand">tariffg4t2</Link>
        <nav>
          <NavLink to="/" end>Home</NavLink>
          <NavLink to="/calculator">Calculator</NavLink>
        </nav>
      </header>
      <main className="container">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/calculator" element={<Calculator />} />
        </Routes>
      </main>
      <footer className="footer">© {new Date().getFullYear()} tariffg4t2</footer>
    </div>
  )
}
