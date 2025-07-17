const express = require('express');
const cors = require('cors');
const app = express();

app.use(cors());
app.use(express.json());

let busPositions = {};

app.post('/api/position', (req, res) => {
    const { busNumber, latitude, longitude } = req.body;

    if (!busNumber || latitude == null || longitude == null) {
        return res.status(400).json({ error: 'Données manquantes' });
    }

    busPositions[busNumber] = {
        latitude,
        longitude,
        timestamp: Date.now()
    };

    res.json({ message: 'Position enregistrée' });
});

app.get('/api/position/:busNumber', (req, res) => {
    const position = busPositions[req.params.busNumber];
    if (!position) return res.status(404).json({ error: 'Bus introuvable' });
    res.json(position);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Serveur en écoute sur le port ${PORT}`);
});
