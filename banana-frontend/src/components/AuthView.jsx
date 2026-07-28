import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ArrowLeft, Terminal } from 'lucide-react';
import { API_BASE_URL } from '../config'; // Using your new config file!

export default function AuthView({ onBack, onLoginSuccess }) {
    const [isLogin, setIsLogin] = useState(true);
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [authError, setAuthError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const handleAuthSubmit = async (e) => {
        e.preventDefault();
        setAuthError('');
        setIsLoading(true);
        const endpoint = isLogin ? '/api/users/login' : '/api/users/register';

        try {
            const response = await fetch(`${API_BASE_URL}${endpoint}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            if (!response.ok) throw new Error(await response.text() || 'Authentication failed.');

            if (isLogin) {
                const data = await response.json();
                // We pass the token and username back up to App.jsx!
                onLoginSuccess(data.token, username);
            } else {
                setIsLogin(true);
                setPassword('');
                alert('Registration successful. Please log in.');
            }
        } catch (err) {
            setAuthError(err.message);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-terminal-900 flex items-center justify-center font-sans relative overflow-hidden selection:bg-banana-500/30">
            <button onClick={onBack} className="absolute top-8 left-8 text-slate-500 hover:text-white flex items-center gap-2 font-bold tracking-widest uppercase text-sm transition-colors z-20">
                <ArrowLeft className="w-4 h-4" /> Back to Home
            </button>
            <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="w-full max-w-md p-8 relative z-10">
                <div className="bg-terminal-800/90 backdrop-blur-xl border border-slate-700/50 p-8 rounded-2xl shadow-2xl">
                    <div className="flex flex-col items-center mb-8">
                        <div className="bg-slate-900 p-4 rounded-full border border-slate-700 mb-4">
                            <Terminal className="w-8 h-8 text-banana-400" />
                        </div>
                        <h1 className="text-2xl font-bold text-white tracking-widest font-mono">
                            {isLogin ? 'SYSTEM LOGIN' : 'NEW REGISTRATION'}
                        </h1>
                    </div>

                    <AnimatePresence>
                        {authError && (
                            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="bg-red-500/10 border border-red-500/50 text-red-400 text-sm font-bold p-3 rounded-lg mb-6 text-center">
                                {authError}
                            </motion.div>
                        )}
                    </AnimatePresence>

                    <form onSubmit={handleAuthSubmit} className="space-y-6">
                        <div>
                            <label className="text-[10px] uppercase tracking-widest text-slate-400 font-bold">Trader Alias</label>
                            <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} className="w-full bg-slate-900/80 border border-slate-600 text-white rounded-lg py-3 px-4 focus:border-banana-400" required />
                        </div>
                        <div>
                            <label className="text-[10px] uppercase tracking-widest text-slate-400 font-bold">Passcode</label>
                            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} className="w-full bg-slate-900/80 border border-slate-600 text-white rounded-lg py-3 px-4 focus:border-banana-400" required />
                        </div>
                        <button type="submit" disabled={isLoading} className="w-full bg-banana-500 hover:bg-banana-400 text-slate-900 font-bold py-3.5 px-4 rounded-lg uppercase tracking-widest mt-2">
                            {isLoading ? 'PROCESSING...' : (isLogin ? 'AUTHENTICATE' : 'REGISTER')}
                        </button>
                    </form>
                    <div className="mt-6 text-center">
                        <button onClick={() => setIsLogin(!isLogin)} className="text-xs text-slate-500 hover:text-banana-400 uppercase tracking-widest font-bold">
                            {isLogin ? 'Create new terminal access' : 'Access existing terminal'}
                        </button>
                    </div>
                </div>
            </motion.div>
        </div>
    );
}