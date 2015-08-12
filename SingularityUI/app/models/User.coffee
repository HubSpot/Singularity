class User extends Backbone.Model
    url: -> "#{ config.apiRoot }/auth/user"

module.exports = User