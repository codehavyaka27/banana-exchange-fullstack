import { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Terminal, Lock, User, ChevronRight, Activity, Wallet, Target, PlusCircle, X, ArrowLeft, Hexagon, Zap, Shield, TrendingUp, AlertTriangle, Briefcase, List as ListIcon, History, Globe, Zap as ZapIcon, Cpu } from 'lucide-react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { createChart, CandlestickSeries } from 'lightweight-charts';

// --- HELPER FUNCTIONS ---
const getUserIdFromToken = (token) => {
    try { return JSON.parse(atob(token.split('.')[1])).userId; } catch (e) { return null; }
};

const parseAsset = (item) => {
    const id = item.cardId;
    const name = item.ticker;
    const price = item.currentPrice || 0;
    return { id, name, price: Number(price), raw: item };
};

const getLogo = (name) => {
    if (!name) return <Activity className="w-8 h-8 text-banana-400" />;
    const upper = name.toUpperCase();
    if (upper.includes('TIKO')) return <Shield className="w-8 h-8 text-blue-400" />;
    if (upper.includes('ABM')) return <Hexagon className="w-8 h-8 text-purple-400" />;
    if (upper.includes('CURSE')) return <Zap className="w-8 h-8 text-red-400" />;
    return <Activity className="w-8 h-8 text-banana-400" />;
};

// --- TRADINGVIEW CANDLESTICK CHART (WITH OHLC AGGREGATOR) ---
const TradingViewChart = ({ currentPrice }) => {
    const chartContainerRef = useRef();
    const chartRef = useRef();
    const seriesRef = useRef();
    const [currentCandle, setCurrentCandle] = useState(null);

    useEffect(() => {
        if (!chartContainerRef.current) return;
        const chart = createChart(chartContainerRef.current, {
            layout: { background: { type: 'solid', color: 'transparent' }, textColor: '#94a3b8' },
            grid: { vertLines: { color: 'rgba(51, 65, 85, 0.5)' }, horzLines: { color: 'rgba(51, 65, 85, 0.5)' } },
            width: chartContainerRef.current.clientWidth,
            height: 350,
            timeScale: { timeVisible: true, secondsVisible: true, borderVisible: false },
            rightPriceScale: { borderVisible: false }
        });

        const candlestickSeries = chart.addSeries(CandlestickSeries, {
            upColor: '#22c55e', downColor: '#ef4444', borderVisible: false, wickUpColor: '#22c55e', wickDownColor: '#ef4444'
        });

        chartRef.current = chart;
        seriesRef.current = candlestickSeries;

        const handleResize = () => chart.applyOptions({ width: chartContainerRef.current.clientWidth });
        window.addEventListener('resize', handleResize);

        return () => { window.removeEventListener('resize', handleResize); chart.remove(); };
    }, []);

    useEffect(() => {
        if (!currentPrice || !seriesRef.current) return;
        const now = Math.floor(Date.now() / 1000);
        const candleTime = Math.floor(now / 5) * 5;
        const tvTime = candleTime;

        setCurrentCandle(prev => {
            if (!prev || prev.time !== tvTime) {
                const newCandle = { time: tvTime, open: currentPrice, high: currentPrice, low: currentPrice, close: currentPrice };
                seriesRef.current.update(newCandle);
                return newCandle;
            } else {
                const updatedCandle = { ...prev, high: Math.max(prev.high, currentPrice), low: Math.min(prev.low, currentPrice), close: currentPrice };
                seriesRef.current.update(updatedCandle);
                return updatedCandle;
            }
        });
    }, [currentPrice]);

    return (
        <div className="w-full h-[350px] relative overflow-hidden rounded-xl bg-slate-900 border border-slate-700 shadow-inner">
            <div ref={chartContainerRef} className="absolute inset-0" />
        </div>
    );
};

function App() {
    // ==========================================
    // --- NEW ROUTING STATE ---
    // 'LANDING' (Public Home), 'AUTH' (Login/Reg), 'APP' (Secure Terminal)
    // ==========================================
    const [currentView, setCurrentView] = useState('LANDING');

    const [isLogin, setIsLogin] = useState(true);
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [authError, setAuthError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [token, setToken] = useState(localStorage.getItem('token') || '');

    const [activeTab, setActiveTab] = useState('MARKETS');
    const [portfolio, setPortfolio] = useState(null);
    const [marketItems, setMarketItems] = useState([]);
    const [orderHistory, setOrderHistory] = useState([]);
    const [tradeMessage, setTradeMessage] = useState(null);
    const [activeAssetId, setActiveAssetId] = useState(null);
    const [tradeAmount, setTradeAmount] = useState('');
    const [showMintModal, setShowMintModal] = useState(false);
    const [mintName, setMintName] = useState('');
    const [mintSupply, setMintSupply] = useState(1000);
    const [mintSeed, setMintSeed] = useState(50000);



    const activeAsset = activeAssetId ? marketItems.find(item => item.id === activeAssetId) : null;
    const fetchPortfolioData = async () => {
        const userId = getUserIdFromToken(token);
        if (!userId) return handleLogout();
        try {
            const res = await fetch(`https://banana-backend1.onrender.com/api/users/${userId}/portfolio`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            // THE FIX: If the backend says "Forbidden" or "Unauthorized", kill the session
            if (res.status === 401 || res.status === 403) {
                showNotification("Session expired. Please log in again.", "error");
                return handleLogout();
            }
            if (res.ok) setPortfolio(await res.json());
        } catch (err) { console.error("Portfolio Sync Error"); }
    };

    const fetchMarketData = async () => {
        try {
            const res = await fetch(`https://banana-backend1.onrender.com/api/cards/market`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            // THE FIX
            if (res.status === 401 || res.status === 403) return handleLogout();
            if (res.ok) {
                const rawData = await res.json();
                setMarketItems(rawData.map(parseAsset));
            }
        } catch (err) { console.error("Market Sync Error"); }
    };

    const fetchOrderHistory = async () => {
        const userId = getUserIdFromToken(token);
        if (!userId) return handleLogout();
        try {
            const res = await fetch(`https://banana-backend1.onrender.com/api/orders/user/${userId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            // THE FIX
            if (res.status === 401 || res.status === 403) return handleLogout();
            if (res.ok) setOrderHistory(await res.json());
        } catch (err) { console.error("Order History Sync Error"); }
    };

    // --- WEBSOCKET MATRIX CONNECTION ---
    useEffect(() => {
        // Double check we actually belong in the app
        if (!token || currentView !== 'APP') return;

        fetchPortfolioData(); fetchMarketData(); fetchOrderHistory();

        const socket = new SockJS('https://banana-backend1.onrender.com/ws-market');
        const stompClient = new Client({
            webSocketFactory: () => socket,
            reconnectDelay: 5000,
            onConnect: () => {
                stompClient.subscribe('/topic/market', (message) => {
                    if (message.body === 'UPDATE') {
                        // THE FIX: Double check local storage. If we logged out, DO NOT FETCH.
                        if (localStorage.getItem('token')) {
                            fetchMarketData(); fetchPortfolioData(); fetchOrderHistory();
                        }
                    }
                });
            },
        });

        stompClient.activate();

        return () => {
            // THE FIX: Unconditionally kill the socket on cleanup.
            // If it is connecting, it will abort. If it is active, it will disconnect.
            stompClient.deactivate();
        };

        // (Ensure you do not put the fetch functions in this dependency array!)
    }, [token, currentView]);

    const showNotification = (text, type = 'success') => {
        setTradeMessage({ text, type });
        setTimeout(() => setTradeMessage(null), 4000);
    };

    const handleMint = async (e) => {
        e.preventDefault();
        setShowMintModal(false);
        showNotification(`INITIALIZING IPO FOR ${mintName}...`, 'info');
        const userId = getUserIdFromToken(token);
        try {
            const response = await fetch(`https://banana-backend1.onrender.com/api/cards/mint`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId, cardName: mintName, totalSupply: parseInt(mintSupply), initialCashSeed: parseFloat(mintSeed) })
            });
            if (!response.ok) throw new Error(await response.text());
            showNotification(`SUCCESS: MINTED ${mintName}! Pool is live.`, 'success');
            setMintName(''); fetchPortfolioData(); fetchMarketData();
        } catch (err) { showNotification(`MINT ERROR: ${err.message}`, 'error'); }
    };

    const handleTrade = async (action) => {
        if (!tradeAmount || tradeAmount <= 0) return showNotification(`ERROR: Enter a valid amount.`, 'error');
        if (!activeAsset) return;
        const userId = getUserIdFromToken(token);
        showNotification(`PROCESSING ${action} ORDER...`, 'info');
        try {
            const response = await fetch(`https://banana-backend1.onrender.com/api/orders/trade`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId, cardId: activeAsset.id, orderType: action, tradeAmount: parseFloat(tradeAmount) })
            });
            if (!response.ok) throw new Error(await response.text());
            showNotification(`SUCCESS: ${action} order executed!`, 'success');
            setTradeAmount('');
        } catch (err) { showNotification(`REJECTED: ${err.message}`, 'error'); }
    };

    const handleAuthSubmit = async (e) => {
        e.preventDefault(); setAuthError(''); setIsLoading(true);
        const endpoint = isLogin ? '/api/users/login' : '/api/users/register';
        try {
            // FIX: Removed the slash after .com so it doesn't double up with the endpoint!
            const response = await fetch(`https://banana-backend1.onrender.com${endpoint}?username=${username}&password=${password}`, { method: 'POST' });
            if (!response.ok) throw new Error(await response.text() || 'Authentication failed.');
            if (isLogin) {
                const data = await response.json();
                localStorage.setItem('token', data.token);
                localStorage.setItem('username', username);
                setToken(data.token);
                setCurrentView('APP'); // <-- DIRECT USER TO APP ON SUCCESS
            } else { setIsLogin(true); setPassword(''); alert('Registration successful. Please log in.'); }
        } catch (err) { setAuthError(err.message); } finally { setIsLoading(false); }
    };

    const handleLogout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('username');
        setToken(''); setPortfolio(null); setActiveAssetId(null);
        setCurrentView('LANDING'); // <-- DIRECT USER TO HOMEPAGE ON LOGOUT
    };

    // --- NEW: THE "ENTER EXCHANGE" ROUTING LOGIC ---
    const handleEnterExchange = () => {
        if (token) setCurrentView('APP'); // Bypass login if token exists
        else setCurrentView('AUTH');      // Go to login if no token
    };

    const MINT_GOAL = 100000;
    const currentCash = portfolio?.walletBalance ? Number(portfolio.walletBalance) : 0;

    const getOwnedQuantity = (asset) => {
        if (!portfolio || !portfolio.inventory || !asset) return 0;
        const item = portfolio.inventory.find(i => i.ticker && i.ticker.toUpperCase() === asset.name.toUpperCase());
        return item && item.quantity ? Number(item.quantity) : 0;
    };

    const holdingsValue = (portfolio?.inventory || []).reduce((total, item) => {
        const marketAsset = marketItems.find(m => m.name.toUpperCase() === item.ticker.toUpperCase());
        const assetPrice = marketAsset ? marketAsset.price : 0;
        return total + (Number(item.quantity) * assetPrice);
    }, 0);

    const totalNetWorth = currentCash + holdingsValue;
    const progressPercent = Math.min((totalNetWorth / MINT_GOAL) * 100, 100);
    const canMint = totalNetWorth >= MINT_GOAL;
    const displayUsername = localStorage.getItem('username') || 'TRADER';

    // ==========================================
    // ROOM 1: THE PUBLIC LANDING PAGE
    // ==========================================
    if (currentView === 'LANDING') {
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
                            <button onClick={() => setCurrentView('AUTH')} className="text-slate-300 font-bold tracking-widest uppercase hover:text-white px-4 py-2 transition-colors text-sm">
                                Log In
                            </button>
                        )}
                        <button onClick={handleEnterExchange} className="bg-banana-500 hover:bg-banana-400 text-slate-900 font-bold px-6 py-2 rounded-lg tracking-widest uppercase transition-all shadow-[0_0_15px_rgba(250,204,21,0.3)] text-sm flex items-center gap-2">
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
                            <button onClick={handleEnterExchange} className="bg-banana-500 hover:bg-banana-400 text-slate-900 font-black text-lg px-8 py-4 rounded-xl tracking-widest uppercase transition-all shadow-[0_0_30px_rgba(250,204,21,0.4)] flex items-center justify-center gap-3">
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

    // ==========================================
    // ROOM 2: AUTHENTICATION (LOGIN/REG)
    // ==========================================
    if (currentView === 'AUTH') {
        return (
            <div className="min-h-screen bg-terminal-900 flex items-center justify-center font-sans relative overflow-hidden selection:bg-banana-500/30">
                <button onClick={() => setCurrentView('LANDING')} className="absolute top-8 left-8 text-slate-500 hover:text-white flex items-center gap-2 font-bold tracking-widest uppercase text-sm transition-colors z-20">
                    <ArrowLeft className="w-4 h-4" /> Back to Home
                </button>
                <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="w-full max-w-md p-8 relative z-10">
                    <div className="bg-terminal-800/90 backdrop-blur-xl border border-slate-700/50 p-8 rounded-2xl shadow-2xl">
                        <div className="flex flex-col items-center mb-8">
                            <div className="bg-slate-900 p-4 rounded-full border border-slate-700 mb-4"><Terminal className="w-8 h-8 text-banana-400" /></div>
                            <h1 className="text-2xl font-bold text-white tracking-widest font-mono">{isLogin ? 'SYSTEM LOGIN' : 'NEW REGISTRATION'}</h1>
                        </div>
                        <AnimatePresence>{authError && <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="bg-red-500/10 border border-red-500/50 text-red-400 text-sm font-bold p-3 rounded-lg mb-6 text-center">{authError}</motion.div>}</AnimatePresence>
                        <form onSubmit={handleAuthSubmit} className="space-y-6">
                            <div><label className="text-[10px] uppercase tracking-widest text-slate-400 font-bold">Trader Alias</label><input type="text" value={username} onChange={(e) => setUsername(e.target.value)} className="w-full bg-slate-900/80 border border-slate-600 text-white rounded-lg py-3 px-4 focus:border-banana-400" required /></div>
                            <div><label className="text-[10px] uppercase tracking-widest text-slate-400 font-bold">Passcode</label><input type="password" value={password} onChange={(e) => setPassword(e.target.value)} className="w-full bg-slate-900/80 border border-slate-600 text-white rounded-lg py-3 px-4 focus:border-banana-400" required /></div>
                            <button type="submit" disabled={isLoading} className="w-full bg-banana-500 hover:bg-banana-400 text-slate-900 font-bold py-3.5 px-4 rounded-lg uppercase tracking-widest mt-2">{isLoading ? 'PROCESSING...' : (isLogin ? 'AUTHENTICATE' : 'REGISTER')}</button>
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

    // ==========================================
    // ROOM 3: THE SECURE APP TERMINAL
    // ==========================================
    return (
        <div className="min-h-screen bg-terminal-900 text-slate-300 font-mono relative selection:bg-banana-500/30">
            <header className="bg-terminal-800 border-b border-slate-700 px-6 py-4 flex flex-col lg:flex-row justify-between items-center sticky top-0 z-40 shadow-lg gap-4">
                <div className="flex items-center gap-6 w-full lg:w-auto">
                    <div onClick={() => setCurrentView('LANDING')} className="flex items-center gap-3 cursor-pointer hover:opacity-80 transition-opacity">
                        <Activity className="text-banana-400 w-6 h-6 animate-pulse" />
                        <h1 className="text-xl font-bold tracking-wider text-white hidden md:block">BANANA_EXCHANGE</h1>
                    </div>

                    <div className="flex bg-slate-900 rounded-lg p-1 border border-slate-700 w-full lg:w-auto">
                        <button onClick={() => { setActiveTab('MARKETS'); setActiveAssetId(null); }} className={`flex-1 md:flex-none flex items-center justify-center gap-2 px-4 py-2 rounded-md text-xs font-bold tracking-widest uppercase transition-colors ${activeTab === 'MARKETS' ? 'bg-slate-700 text-white shadow' : 'text-slate-500 hover:text-slate-300'}`}><TrendingUp className="w-4 h-4" /> Markets</button>
                        <button onClick={() => { setActiveTab('DASHBOARD'); setActiveAssetId(null); }} className={`flex-1 md:flex-none flex items-center justify-center gap-2 px-4 py-2 rounded-md text-xs font-bold tracking-widest uppercase transition-colors ${activeTab === 'DASHBOARD' ? 'bg-slate-700 text-white shadow' : 'text-slate-500 hover:text-slate-300'}`}><History className="w-4 h-4" /> Dashboard</button>
                    </div>


                </div>

                <div className="flex items-center gap-4 text-sm font-bold w-full lg:w-auto justify-between lg:justify-end">
                    <div className="text-banana-400 uppercase tracking-widest flex items-center gap-2"><User className="w-4 h-4" /> {displayUsername}</div>
                    <div className="bg-slate-900 px-4 py-2 rounded-lg border border-slate-700 flex flex-col items-end">
                        <span className="text-[10px] text-slate-500 uppercase tracking-widest">Net Worth</span>
                        <span className="text-green-400">${totalNetWorth.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
                    </div>
                    <button onClick={handleLogout} className="text-slate-500 hover:text-red-400 transition-colors uppercase tracking-widest">Logout</button>
                </div>
            </header>

            <div className="fixed top-24 left-1/2 -translate-x-1/2 z-50 w-full max-w-md px-4 pointer-events-none">
                <AnimatePresence>
                    {tradeMessage && (
                        <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -20 }} className={`p-4 rounded-lg border font-bold text-center tracking-widest shadow-2xl backdrop-blur-md ${ tradeMessage.type === 'error' ? 'bg-red-900/90 border-red-500 text-red-200' : tradeMessage.type === 'info' ? 'bg-blue-900/90 border-blue-500 text-blue-200' : 'bg-green-900/90 border-green-500 text-green-200' }`}>{tradeMessage.text}</motion.div>
                    )}
                </AnimatePresence>
            </div>

            <div className="max-w-5xl mx-auto p-4 md:p-8">
                <AnimatePresence mode="wait">

                    {activeTab === 'MARKETS' && !activeAsset && (
                        <motion.div key="market" initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }}>
                            <section className="bg-terminal-800 border border-slate-700 p-6 rounded-xl shadow-lg mb-8">
                                <div className="flex justify-between text-xs mb-2 uppercase tracking-widest font-bold text-slate-400"><span className="flex items-center gap-2"><Target className="w-4 h-4 text-banana-400"/> Syndicate IPO Access</span><span className={canMint ? 'text-banana-400' : ''}>{progressPercent.toFixed(1)}%</span></div>
                                <div className="w-full bg-slate-900 h-3 rounded-full overflow-hidden border border-slate-700 relative mb-4"><motion.div initial={{ width: 0 }} animate={{ width: `${progressPercent}%` }} className={`absolute top-0 left-0 h-full transition-all duration-1000 ${canMint ? 'bg-banana-400' : 'bg-blue-500'}`} /></div>
                                <button onClick={() => setShowMintModal(true)} disabled={!canMint} className={`w-full py-3 rounded-lg font-bold uppercase tracking-widest flex justify-center items-center gap-2 transition-all ${canMint ? 'bg-banana-500 hover:bg-banana-400 text-slate-900 shadow-[0_0_20px_rgba(234,179,8,0.3)]' : 'bg-slate-800 text-slate-600 border border-slate-700'}`}><PlusCircle className="w-5 h-5" /> Initialize New Asset Deployment</button>
                            </section>
                            <h2 className="text-xl font-bold uppercase tracking-widest text-slate-400 mb-4 flex items-center gap-2"><TrendingUp className="w-5 h-5"/> Live Markets</h2>
                            {marketItems.length === 0 ? <div className="text-center text-slate-500 py-16 border border-dashed border-slate-700 rounded-xl bg-terminal-800/50 tracking-widest">SYNCING LIQUIDITY POOLS...</div> : (
                                <div className="space-y-3">
                                    {marketItems.map((item) => (
                                        <div key={item.id} onClick={() => { setActiveAssetId(item.id); setTradeAmount(''); }} className="bg-terminal-800 border border-slate-700 p-4 rounded-xl flex items-center justify-between hover:border-banana-400/50 hover:bg-slate-800 transition-all cursor-pointer group">
                                            <div className="flex items-center gap-4">
                                                <div className="bg-slate-900 p-3 rounded-lg border border-slate-700 group-hover:border-banana-400/30 transition-colors">{getLogo(item.name)}</div>
                                                <div>
                                                    <h3 className="text-lg font-bold text-white tracking-wider">{item.name}</h3>
                                                    <span className="text-xs text-slate-500 uppercase font-bold tracking-widest">Owned: {getOwnedQuantity(item).toLocaleString(undefined, { maximumFractionDigits: 4 })}</span>
                                                </div>
                                            </div>
                                            <div className="text-right">
                                                <div className="text-xl font-bold text-green-400">${item.price.toFixed(2)}</div>
                                                <div className="text-xs text-banana-400 font-bold tracking-widest flex items-center justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity">TRADE <ChevronRight className="w-3 h-3"/></div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </motion.div>
                    )}

                    {activeTab === 'MARKETS' && activeAsset && (
                        <motion.div key="desk" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 20 }}>
                            <button onClick={() => setActiveAssetId(null)} className="flex items-center gap-2 text-slate-400 hover:text-white mb-6 font-bold tracking-widest uppercase text-sm transition-colors"><ArrowLeft className="w-4 h-4" /> Back to Markets</button>
                            <div className="bg-terminal-800 border border-slate-700 rounded-2xl shadow-2xl overflow-hidden">
                                <div className="bg-slate-900/80 p-8 border-b border-slate-700 flex flex-col md:flex-row justify-between items-center gap-6">
                                    <div className="flex items-center gap-4">
                                        <div className="bg-slate-800 p-4 rounded-xl border border-slate-600 shadow-lg">{getLogo(activeAsset.name)}</div>
                                        <div><h2 className="text-3xl font-bold text-white tracking-widest">{activeAsset.name}</h2><div className="text-sm text-slate-400 uppercase tracking-widest font-bold mt-1">Live AMM Price</div></div>
                                    </div>
                                    <div className="text-5xl font-bold text-green-400 tracking-tighter">${activeAsset.price.toFixed(2)}</div>
                                </div>
                                <div className="border-b border-slate-700 p-4 bg-slate-900/50"><TradingViewChart currentPrice={activeAsset.price} /></div>
                                <div className="p-8">
                                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
                                        <div className="bg-slate-900/50 p-4 rounded-xl border border-slate-700">
                                            <div className="text-xs text-slate-500 uppercase tracking-widest font-bold mb-1 flex items-center gap-2"><Wallet className="w-3 h-3"/> Your Cash</div>
                                            <div className="text-xl font-bold text-white">${currentCash.toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
                                        </div>
                                        <div className="bg-slate-900/50 p-4 rounded-xl border border-slate-700">
                                            <div className="text-xs text-slate-500 uppercase tracking-widest font-bold mb-1 flex items-center gap-2"><Briefcase className="w-3 h-3"/> Position</div>
                                            <div className="text-xl font-bold text-white">{getOwnedQuantity(activeAsset).toLocaleString(undefined, { maximumFractionDigits: 4 })} <span className="text-sm text-slate-500">QTY</span></div>
                                            <div className="text-xs text-slate-400 mt-1">Value: ${(getOwnedQuantity(activeAsset) * activeAsset.price).toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
                                        </div>
                                        {(() => {
                                            const invItem = portfolio?.inventory?.find(i => i.ticker.toUpperCase() === activeAsset.name.toUpperCase());
                                            const avgPrice = invItem?.averagePurchasePrice ? Number(invItem.averagePurchasePrice) : 0;
                                            const qty = getOwnedQuantity(activeAsset);
                                            const pnl = (activeAsset.price - avgPrice) * qty;
                                            const isProfit = pnl >= 0;
                                            return (
                                                <div className={`bg-slate-900/50 p-4 rounded-xl border ${qty > 0 ? (isProfit ? 'border-green-500/50' : 'border-red-500/50') : 'border-slate-700'}`}>
                                                    <div className="text-xs text-slate-500 uppercase tracking-widest font-bold mb-1 flex items-center gap-2"><TrendingUp className="w-3 h-3"/> Unrealized PnL</div>
                                                    {qty > 0 ? <><div className={`text-xl font-bold ${isProfit ? 'text-green-400' : 'text-red-400'}`}>{isProfit ? '+' : ''}${Math.abs(pnl).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</div><div className="text-xs text-slate-400 mt-1">Avg Entry: ${avgPrice.toFixed(4)}</div></> : <div className="text-xl font-bold text-slate-600">--</div>}
                                                </div>
                                            );
                                        })()}
                                    </div>
                                    <div className="mb-8">
                                        <label className="text-sm uppercase text-slate-400 font-bold mb-2 block tracking-widest">Execution Amount</label>
                                        <div className="relative">
                                            <input type="number" min="0.01" step="0.01" placeholder="Enter amount..." value={tradeAmount} onChange={(e) => setTradeAmount(e.target.value)} className="w-full bg-slate-900 border-2 border-slate-600 rounded-xl py-4 px-6 text-white focus:border-banana-400 focus:ring-0 focus:outline-none text-2xl font-bold transition-colors" />
                                            <div className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 font-bold text-sm tracking-widest">( $ FOR BUY / QTY FOR SELL )</div>
                                        </div>
                                    </div>
                                    <div className="grid grid-cols-2 gap-4">
                                        <button onClick={() => handleTrade('BUY')} className="bg-green-500 hover:bg-green-400 text-slate-900 py-4 rounded-xl font-black text-xl tracking-widest uppercase transition-all shadow-[0_0_20px_rgba(34,197,94,0.2)]">BUY</button>
                                        <button onClick={() => handleTrade('SELL')} className="bg-red-500 hover:bg-red-400 text-white py-4 rounded-xl font-black text-xl tracking-widest uppercase transition-all shadow-[0_0_20px_rgba(239,68,68,0.2)]">SELL</button>
                                    </div>
                                </div>
                            </div>
                        </motion.div>
                    )}

                    {activeTab === 'DASHBOARD' && (
                        <motion.div key="dashboard" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 20 }} className="space-y-6">
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                <div className="bg-terminal-800 border border-slate-700 p-6 rounded-xl"><div className="text-xs text-slate-500 uppercase tracking-widest font-bold mb-2">Total Net Worth</div><div className="text-3xl font-bold text-green-400">${totalNetWorth.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</div></div>
                                <div className="bg-terminal-800 border border-slate-700 p-6 rounded-xl"><div className="text-xs text-slate-500 uppercase tracking-widest font-bold mb-2">Liquid Cash</div><div className="text-3xl font-bold text-white">${currentCash.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</div></div>
                                <div className="bg-terminal-800 border border-slate-700 p-6 rounded-xl"><div className="text-xs text-slate-500 uppercase tracking-widest font-bold mb-2">Total Trades Executed</div><div className="text-3xl font-bold text-banana-400">{orderHistory.length}</div></div>
                            </div>
                            <div className="bg-terminal-800 border border-slate-700 rounded-xl overflow-hidden shadow-lg">
                                <div className="bg-slate-900/80 p-4 border-b border-slate-700 flex items-center gap-3"><ListIcon className="text-banana-400 w-5 h-5" /><h2 className="text-sm font-bold uppercase tracking-widest text-slate-300">Terminal Trade Log</h2></div>
                                {orderHistory.length === 0 ? <div className="p-12 text-center text-slate-500 font-bold tracking-widest uppercase text-sm border-t border-slate-700 border-dashed">No trading activity detected.</div> : (
                                    <div className="overflow-x-auto">
                                        <table className="w-full text-left border-collapse">
                                            <thead><tr className="bg-slate-900/50 text-[10px] uppercase tracking-widest text-slate-500 border-b border-slate-700"><th className="p-4 font-bold">Date / Time</th><th className="p-4 font-bold">Type</th><th className="p-4 font-bold">Asset</th><th className="p-4 font-bold">Avg Entry</th><th className="p-4 font-bold">Exec Price</th><th className="p-4 font-bold">Quantity</th><th className="p-4 font-bold text-right">Realized PnL</th></tr></thead>
                                            <tbody className="text-sm font-bold tracking-wider">
                                            {orderHistory.map((order) => {
                                                const orderDate = order.timestamp ? new Date(order.timestamp) : new Date();
                                                const isSell = order.orderType === 'SELL';
                                                const pnl = order.realizedPnl ? Number(order.realizedPnl) : 0;
                                                return (
                                                    <tr key={order.id} className="border-b border-slate-700/50 hover:bg-slate-800/50 transition-colors">
                                                        <td className="p-4"><div className="text-slate-300">{orderDate.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}</div><div className="text-[10px] text-slate-500">{orderDate.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit', second: '2-digit' })}</div></td>
                                                        <td className="p-4"><span className={`px-2 py-1 rounded text-[10px] uppercase tracking-widest ${order.orderType === 'BUY' ? 'bg-green-500/10 text-green-400 border border-green-500/20' : 'bg-red-500/10 text-red-400 border border-red-500/20'}`}>{order.orderType}</span></td>
                                                        <td className="p-4 text-white">{order.ticker}</td>
                                                        <td className="p-4 text-slate-400">{isSell && order.entryPrice ? `$${Number(order.entryPrice).toFixed(4)}` : '--'}</td>
                                                        <td className="p-4 text-slate-300">${Number(order.price).toFixed(4)}</td>
                                                        <td className="p-4 text-slate-300">{Number(order.quantity).toLocaleString(undefined, { maximumFractionDigits: 2 })}</td>
                                                        <td className="p-4 text-right">{isSell ? <span className={pnl >= 0 ? 'text-green-400' : 'text-red-400'}>{pnl >= 0 ? '+' : ''}${pnl.toFixed(2)}</span> : <span className="text-slate-600">--</span>}</td>
                                                    </tr>
                                                );
                                            })}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </div>
                        </motion.div>
                    )}
                </AnimatePresence>
            </div>

            <AnimatePresence>
                {showMintModal && (
                    <div className="fixed inset-0 bg-black/80 backdrop-blur-md flex items-center justify-center z-50 p-4">
                        <motion.div initial={{ opacity: 0, scale: 0.95, y: 20 }} animate={{ opacity: 1, scale: 1, y: 0 }} exit={{ opacity: 0, scale: 0.95, y: 20 }} className="bg-terminal-800 border border-banana-500/30 p-8 rounded-2xl shadow-[0_0_50px_rgba(234,179,8,0.1)] w-full max-w-md relative">
                            <button onClick={() => setShowMintModal(false)} className="absolute top-4 right-4 text-slate-500 hover:text-red-400 p-1"><X className="w-6 h-6" /></button>
                            <div className="flex items-center gap-3 mb-6"><PlusCircle className="text-banana-400 w-8 h-8" /><h2 className="text-2xl font-bold tracking-widest text-white uppercase">Initialize Asset</h2></div>
                            <form onSubmit={handleMint} className="space-y-5">
                                <div><label className="text-xs uppercase tracking-widest text-slate-400 mb-1 block font-bold">Asset Ticker</label><input type="text" value={mintName} onChange={(e) => setMintName(e.target.value.toUpperCase())} className="w-full bg-slate-900 border border-slate-600 rounded-lg p-3 text-white focus:border-banana-400 focus:outline-none uppercase" required /></div>
                                <button type="submit" className="w-full bg-banana-500 hover:bg-banana-400 text-slate-900 font-bold py-4 rounded-lg transition-all mt-6 uppercase tracking-widest">Deploy to Network</button>
                            </form>
                        </motion.div>
                    </div>
                )}
            </AnimatePresence>
        </div>
    );
}

export default App;