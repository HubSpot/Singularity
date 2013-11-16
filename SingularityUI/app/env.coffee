if window.location.hostname is 'localhost'
    env =
        env: 'local'
        SINGULARITY_BASE: 'http://heliograph.iad01.hubspot-networks.net:7005'
else
    env =
        env: 'prod'
        SINGULARITY_BASE: ''

module.exports = env