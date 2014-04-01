Model = require './model'

class User extends Model

    localStorage: new Backbone.LocalStorage('SingularityUser')

module.exports = User