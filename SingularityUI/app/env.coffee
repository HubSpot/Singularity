if window.location.hostname.substr(0, 'local'.length) is 'local'
    env =
        env: 'local'
        SINGULARITY_BASE: 'http://heliograph.iad01.hubspot-networks.net:7005'
else
    env =
        env: 'prod'
        SINGULARITY_BASE: ''

module.exports = env