const express = require('express');
const cors = require('cors');
const app = express();

app.use(cors());
app.use(express.json());

let busPositions = {};

// Enregistrer ou mettre à jour la position
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

// Récupérer la position d’un bus
app.get('/api/position/:busNumber', (req, res) => {
    const position = busPositions[req.params.busNumber];
    if (!position) return res.status(404).json({ error: 'Bus introuvable' });
    res.json(position);
});

// Nouvelle route pour arrêter le partage
app.post('/api/stopSharing', (req, res) => {
    const { busNumber } = req.body;

    if (!busNumber) {
        return res.status(400).json({ error: 'Numéro de bus manquant' });
    }

    if (busPositions[busNumber]) {
        // Supprimer la position du bus du stockage
        delete busPositions[busNumber];
        return res.json({ message: `Partage arrêté pour le bus ${busNumber}` });
    } else {
        return res.status(404).json({ error: 'Bus introuvable' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Serveur en écoute sur le port ${PORT}`);
});
