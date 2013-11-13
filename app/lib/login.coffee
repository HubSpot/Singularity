class Login

    constructor: (@env) ->
        @verify_url = "https://#{env.LOGIN_BASE}/login/api-verify?_=#{+new Date()}&portalId=#{ env.LOGIN_PORTAL }&includeGates=false"

    verifyUser: (onSuccess) =>
        $.ajax
            type: 'GET'
            url: @verify_url
            dataType: 'json'
            crossDomain: true
            data: {}
            xhrFields:
                withCredentials: true
            success: (data) =>
                @context = data
                @env.portalId = data.portal.portal_id
                onSuccess data
            error: =>
                window.location = "https://#{@env.LOGIN_BASE}/login/?loginRedirectUrl=#{window.location}"

module.exports = Login
