class User extends Backbone.Model

    fetch: ->
        @set JSON.parse localStorage.getItem 'singularityUser'

    save: ->
        localStorage.setItem 'singularityUser', JSON.stringify @attributes

module.exports = User