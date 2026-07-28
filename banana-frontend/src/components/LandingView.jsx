import { motion } from 'framer-motion';
import { Activity, ChevronRight, ArrowLeft, Globe, Cpu, TrendingUp, Zap as ZapIcon } from 'lucide-react';

export default function LandingView({ onEnter, onLoginClick, token }) {
    return (
        <div className="min-h-screen bg-terminal-900 text-white font-sans selection:bg-banana-500/30 overflow-hidden relative">
            {/* Background Gradients */}
            <div className="absolute top-0 left-0 w-full h-full overflow-hidden z-0 pointer-events-none">
                <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-banana-500/10 blur-[120px] rounded-full"></div>
                <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-blue-500/10 blur-[120px] rounded-full"></div>
            </div>

            {/* Public Header */}
            <header className="relative z-10 flex justify-between items-center py-6 px-8 max-w-7xl mx-auto border-b border-slate-800">
                <div className="flex items-center gap-3">
                    <Activity className="text-banana-400 w-8 h-8" />
                    <h1 className="text-2xl font-black tracking-widest font-mono">BANANA_EXCHANGE</h1>
                </div>
                <div className="flex gap-4">
                    {!token && (
                        <button onClick={onLoginClick} className="text-slate-300 font-bold tracking-widest uppercase hover:text-white px-4 py-2 transition-colors text-sm">
                            Log In
                        </button>
                    )}
                    <button onClick={onEnter} className="bg-banana-500 hover:bg-banana-400 text-slate-900 font-bold px-6 py-2 rounded-lg tracking-widest uppercase transition-all shadow-[0_0_15px_rgba(250,204,21,0.3)] text-sm flex items-center gap-2">
                        {token ? 'Open Terminal' : 'Start Trading'} <ChevronRight className="w-4 h-4" />
                    </button>
                </div>
            </header>

            {/* Hero Section */}
            <main className="relative z-10 max-w-7xl mx-auto px-8 pt-24 pb-16 flex flex-col items-center text-center">
                <motion.div initial={{ opacity: 0, y: 30 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }} className="max-w-4xl">
                    <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full border border-banana-500/30 bg-banana-500/10 text-banana-400 text-xs font-bold uppercase tracking-widest mb-8">
                        <ZapIcon className="w-4 h-4" /> V2.0 Algorithmic Engine Live
                    </div>
                    <h2 className="text-5xl md:text-7xl font-black tracking-tight mb-6 leading-tight">
                        Trade the <span className="text-transparent bg-clip-text bg-gradient-to-r from-banana-400 to-yellow-200">Sine Wave.</span><br/>Beat the Machine.
                    </h2>
                    <p className="text-xl text-slate-400 mb-10 max-w-2xl mx-auto leading-relaxed">
                        Experience the world's first high-frequency crypto simulator driven by a dynamic mathematical market bot, powered by automated market maker (AMM) liquidity pools.
                    </p>
                    <div className="flex flex-col sm:flex-row gap-4 justify-center">
                        <button onClick={onEnter} className="bg-banana-500 hover:bg-banana-400 text-slate-900 font-black text-lg px-8 py-4 rounded-xl tracking-widest uppercase transition-all shadow-[0_0_30px_rgba(250,204,21,0.4)] flex items-center justify-center gap-3">
                            Access Trading Desk <ArrowLeft className="w-5 h-5 rotate-180" />
                        </button>
                    </div>
                </motion.div>

                {/* Feature Grid */}
                <motion.div initial={{ opacity: 0, y: 40 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6, delay: 0.2 }} className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-24 w-full text-left">
                    <div className="bg-slate-900/50 border border-slate-800 p-8 rounded-2xl backdrop-blur-sm">
                        <Globe className="w-10 h-10 text-blue-400 mb-4" />
                        <h3 className="text-xl font-bold mb-2 tracking-wide">WebSocket Real-Time Data</h3>
                        <p className="text-slate-500">Zero-latency data pipelines push precise market ticks directly to your browser the exact millisecond trades are executed.</p>
                    </div>
                    <div className="bg-slate-900/50 border border-slate-800 p-8 rounded-2xl backdrop-blur-sm">
                        <Cpu className="w-10 h-10 text-purple-400 mb-4" />
                        <h3 className="text-xl font-bold mb-2 tracking-wide">Autonomous Market Bots</h3>
                        <p className="text-slate-500">Compete against an algorithmic bot running on a cyclical sine-wave, generating organic bull and bear market psychology.</p>
                    </div>
                    <div className="bg-slate-900/50 border border-slate-800 p-8 rounded-2xl backdrop-blur-sm">
                        <TrendingUp className="w-10 h-10 text-green-400 mb-4" />
                        <h3 className="text-xl font-bold mb-2 tracking-wide">Institutional 0.2% Fees</h3>
                        <p className="text-slate-500">Day-trade and scalp with tight spreads. Our fractional AMM formula mathematically balances liquidity while protecting capital.</p>
                    </div>
                </motion.div>
            </main>
        </div>
    );
}