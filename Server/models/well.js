const { DataTypes } = require('sequelize');

module.exports = (sequelize) => {
    

    const Well = sequelize.define('Well', {
        wellId: {
            type: DataTypes.STRING,
            allowNull: false,
            unique: true,
            primaryKey: true
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
        wellImages: { // A list of images associated with the well
            type: DataTypes.JSON, // Store as JSON array of ImageData objects
            allowNull: true,
            defaultValue: [],
            get() {
                const rawValue = this.getDataValue('wellImages');
                return rawValue ? JSON.parse(rawValue) : [];
            },
            set(value) {
                this.setDataValue('wellImages', JSON.stringify(value));
            }
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
    const ImageData = sequelize.define('ImageData', {
        imageNumber: {
            type: DataTypes.STRING,
            allowNull: false
        },
        description: {
            type: DataTypes.STRING,
            allowNull: true
        },
        base64: {
            type: DataTypes.BLOB, //The actual image data
        },
        uploadDate:{
            type: DataTypes.DATE,
            allowNull: false
        },
        fileSize:{
            type: DataTypes.INTEGER,
            allowNull: false
        },
    });

    Well.ImageData = ImageData; // Make ImageData accessible via Well.ImageData

    return Well;
};