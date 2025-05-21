const { DataTypes } = require('sequelize');

module.exports = (sequelize) => {
//TODO : fix the fact there is a missing LoginToken : the LoginToken is to ensure the user is properly connected on the app and is not trying to access from a web browser. There should be a user ID and a loginToken that is new each time the user logs in.
    const User = sequelize.define('User', {
        userId: {
            type: DataTypes.UUID,
            defaultValue: DataTypes.UUIDV4,
            primaryKey: true
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
        lastName: {
            type: DataTypes.STRING(64),
            allowNull: false
        },
        username: {
            type: DataTypes.STRING(64),
            allowNull: false,
            unique: true
        },
        role: {
            type: DataTypes.STRING(64),
            allowNull: false,
            defaultValue: 'user'
        },
        location: {
            type: DataTypes.JSON,
            allowNull: true,
            defaultValue: null
        },
        waterNeeds: {
            type: DataTypes.JSON,
            allowNull: true,
            defaultValue: null
        },
        lastActive: {
            type: DataTypes.DATE,
            defaultValue: DataTypes.NOW
        },
        registrationDate: {
            type: DataTypes.DATE,
            defaultValue: DataTypes.NOW
        },
        notificationPreferences: {
            type: DataTypes.JSON,
            allowNull: true,
            defaultValue: {
                weatherAlerts: true,
                wellUpdates: true,
                nearbyUsers: true
            }
        }
    });

    return User;
};