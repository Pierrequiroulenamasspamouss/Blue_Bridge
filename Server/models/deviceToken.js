const { DataTypes } = require('sequelize');

module.exports = (sequelize) => {
    const DeviceToken = sequelize.define('DeviceToken', {

        tokenId: {
            type: DataTypes.UUID,
            defaultValue: DataTypes.UUIDV4,
            primaryKey: true
        },
        userId: {
            type: DataTypes.UUID,
            allowNull: false,
            references: {
                model: 'Users',
                key: 'userId'
            }
        },
        token: {
            type: DataTypes.STRING(255),
            allowNull: false,
            unique: true
        },
        deviceType: {
            type: DataTypes.STRING(20),
            allowNull: false,
            defaultValue: 'android'
        },
        lastUsed: {
            type: DataTypes.DATE,
            defaultValue: DataTypes.NOW
        },
        isActive: {
            type: DataTypes.BOOLEAN,
            defaultValue: true
        }
    });

    return DeviceToken;
}; 