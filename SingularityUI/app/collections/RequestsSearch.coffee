Requests = require './Requests'

class RequestsSearch extends Requests

    comparator: 'createdAt'

    url: => "#{ config.apiRoot }/history/requests/search?count=6&#{ $.param @params }&requestIdLike=#{ @query }"

    initialize: (models, {@query, @params}) =>

module.exports = RequestsSearch
