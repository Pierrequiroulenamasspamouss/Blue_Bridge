const { DataTypes } = require('sequelize');

module.exports = (sequelize) => {
    const Well = sequelize.define('Well', {
        wellId: {
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
        wellStatus: {
            type: DataTypes.STRING,
            allowNull: false,
            defaultValue: 'Unknown'
        },
        waterQuality: {
            type: DataTypes.JSON, // Store as JSON object {ph, turbidity, tds}
            allowNull: true,
            defaultValue: null
        },
        wellImages: {
            type: DataTypes.TEXT,
            allowNull: true,
            defaultValue: '[]'
        },
        createdAt:{
            type: DataTypes.DATE,
            allowNull: false,
            defaultValue: DataTypes.NOW
        },
        updatedAt:{
            type: DataTypes.DATE,
            allowNull: false,
            defaultValue: DataTypes.NOW
        }
    });


    return Well;
}; 