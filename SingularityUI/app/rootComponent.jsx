import React from 'react';

const rootComponent = (title, refresh, Wrapped) => class extends React.Component {

  constructor(props) {
    super(props);
    _.bindAll(this, 'startRefreshInterval', 'stopRefreshInterval');
    this.state = {
      loading: typeof refresh === 'function'
    };
  }

  componentDidMount() {
    document.title = `${title} - ${config.title}`;

    if (refresh) {
      const promise = refresh(this.props);
      if (promise) {
        promise.then(() => {
          this.setState({
            loading: false
          });
        });
      }
    }

    this.startRefreshInterval();
    window.addEventListener('blur', this.stopRefreshInterval);
    window.addEventListener('focus', this.startRefreshInterval);
  }

  componentWillUnmount() {
    this.stopRefreshInterval();
    window.removeEventListener('blur', this.stopRefreshInterval);
    window.removeEventListener('focus', this.startRefreshInterval);
  }

  startRefreshInterval() {
    refresh(this.props);
    this.refreshInterval = setInterval(() => refresh(this.props), config.globalRefreshInterval);
  }

  stopRefreshInterval() {
    clearInterval(this.refreshInterval);
  }


  render() {
    const loader = this.state.loading && <div className="page-loader fixed" />;
    const page = !this.state.loading && <Wrapped {...this.props} />;
    return (
      <div>
        {loader}
        {page}
      </div>
    );
  }
};

export default rootComponent;
