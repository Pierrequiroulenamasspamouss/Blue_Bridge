    const { DataTypes } = require('sequelize');

    module.exports = (sequelize) => {
        const User = sequelize.define('User', {
            userId: {
                type: DataTypes.UUID,
                defaultValue: DataTypes.UUIDV4, // Automatically generate a UUID
                primaryKey: true
            },
            phoneNumber: {
                type: DataTypes.STRING(20),
                allowNull: true
            },
            email: {
                type: DataTypes.STRING(120),
                allowNull: false,
                unique: true
            },
            password: {
                type: DataTypes.STRING(128),
                allowNull: false
            },
            firstName: {
                type: DataTypes.STRING(64),
                allowNull: false
            },
            loginToken: {
                type: DataTypes.STRING(128),
                allowNull: true
            },

            lastName: {
                type: DataTypes.STRING(64),
                allowNull: false
            },
            username: {
                type: DataTypes.STRING(64),
                allowNull: false,

            },
            role: {
                type: DataTypes.STRING(64),
                allowNull: false
            },
            themePreference: {
                type: DataTypes.INTEGER,
                allowNull: false
            },
            latitude: {
                type: DataTypes.FLOAT,
                allowNull: true
            },
            longitude: {
                type: DataTypes.FLOAT,
                allowNull: true
            },
            location: {
                type: DataTypes.JSON, // Store as JSON object {latitude, longitude, lastUpdated}
                allowNull: true,
                defaultValue: null
            },
            waterNeeds: {
                type: DataTypes.JSON, // Store as JSON array or object
                allowNull: true
            },
            isWellOwner: {
                type: DataTypes.BOOLEAN,
                allowNull: false,
                defaultValue: false
            },
            lastActive: {
                type: DataTypes.DATE,
                defaultValue: DataTypes.NOW
            }
        });
        return User;
    };