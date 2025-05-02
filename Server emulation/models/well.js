const { DataTypes } = require('sequelize');

module.exports = (sequelize) => {
    const Well = sequelize.define('Well', {
        espId: {
            type: DataTypes.STRING,
            allowNull: false,
            unique: true
        },
        wellName: {
            type: DataTypes.STRING,
            allowNull: false
        },
        wellOwner: {
            type: DataTypes.STRING,
            allowNull: true
        },
        wellLocation: {
            type: DataTypes.JSON, // Store as JSON object {latitude, longitude}
            allowNull: true,
            defaultValue: null
        },
        wellWaterType: {
            type: DataTypes.STRING,
            allowNull: true,
            defaultValue: 'Clean'
        },
        wellCapacity: {
            type: DataTypes.FLOAT,
            allowNull: false,
            defaultValue: 0.0
        },
        wellWaterLevel: {
            type: DataTypes.FLOAT,
            allowNull: false,
            defaultValue: 0.0
        },
        wellWaterConsumption: {
            type: DataTypes.FLOAT,
            allowNull: false,
            defaultValue: 0.0
        },
        extraData: {
            type: DataTypes.JSON,
            allowNull: true,
            defaultValue: {}
        },
        wellStatus: {
            type: DataTypes.STRING,
            allowNull: false,
            defaultValue: 'Unknown'
        },
        lastUpdated: {
            type: DataTypes.DATE,
            allowNull: true,
            defaultValue: DataTypes.NOW
        },
        waterQuality: {
            type: DataTypes.JSON, // Store as JSON object {ph, turbidity, tds}
            allowNull: true,
            defaultValue: null
        }
    });

    // Define associations
    Well.associate = (models) => {
        Well.belongsTo(models.User, {
            foreignKey: 'ownerId',
            as: 'owner'
        });
    };

    return Well;
}; 