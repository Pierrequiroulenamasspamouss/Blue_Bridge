const { DataTypes } = require('sequelize');

module.exports = (sequelize) => {
    const DeviceToken = sequelize.define('DeviceToken', {
        userId: {
            type: DataTypes.UUID,
            allowNull: false,
            primaryKey: true,
            references: {
                model: 'Users',
                key: 'userId'
            }
        },
        token: {
            type: DataTypes.STRING(255),
            allowNull: false,
            primaryKey: true
        },
        deviceType: {
            type: DataTypes.STRING(20),
            allowNull: true,
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
    }, {
        tableName: 'device_tokens',
        timestamps: true
    });

    return DeviceToken;
}; 