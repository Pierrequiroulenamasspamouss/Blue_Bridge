const { DataTypes } = require('sequelize');

module.exports = (sequelize) => {
  const User = sequelize.define('User', {
    userId: {
      type: DataTypes.UUID,
      defaultValue: DataTypes.UUIDV4,
      primaryKey: true,
      allowNull: false
    },
    email: {
      type: DataTypes.STRING,
      allowNull: false,
      unique: true,
      validate: { isEmail: true }
    },
    password: {
      type: DataTypes.STRING,
      allowNull: false
    },
    firstName: {
      type: DataTypes.STRING,
      allowNull: false
    },
    lastName: {
      type: DataTypes.STRING,
      allowNull: false
    },
    username: DataTypes.STRING,
    role: {
      type: DataTypes.STRING,
      defaultValue: 'user'
    },
    location: {
      type: DataTypes.JSON,
      defaultValue: null
    },
    waterNeeds: {
      type: DataTypes.JSON,
      defaultValue: []
    },
    notificationPreferences: {
      type: DataTypes.JSON,
      defaultValue: {
        weatherAlerts: true,
        wellUpdates: true,
        nearbyUsers: true
      }
    },
    loginToken: DataTypes.STRING,
    phoneNumber: DataTypes.STRING,
    themePreference: {
      type: DataTypes.INTEGER,
      defaultValue: 0
    },
    lastActive: DataTypes.DATE,
    isActive: {
      type: DataTypes.BOOLEAN,
      defaultValue: true
    },

    registrationDate : {
      type: DataTypes.DATE,
      defaultValue: DataTypes.NOW
    },
    isWellOwner: {
      type: DataTypes.BOOLEAN,
      defaultValue: false
    }
  }, {
    tableName: 'users',
    timestamps: true,
    underscored: false,
    freezeTableName: true
  });

  User.associate = (models) => {
    User.hasMany(models.DeviceToken, {
      foreignKey: 'userId',
      as: 'deviceTokens'
    });
  };

  return User;
};