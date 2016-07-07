import React from 'react';

const rootComponent = (Wrapped, title, refresh = _.noop) => class extends React.Component {

  constructor(props) {
    super(props);
    _.bindAll(this, 'handleBlur', 'handleFocus');
    this.state = {
      loading: refresh !== _.noop
    };
  }

  componentDidMount() {
    document.title = `${title} - ${config.title}`;

    const promise = refresh(this.props);
    if (promise) {
      promise.then(() => {
        this.setState({
          loading: false
        });
      });
    }

    this.startRefreshInterval();
    window.addEventListener('blur', this.handleBlur);
    window.addEventListener('focus', this.handleFocus);
  }

  componentWillUnmount() {
    this.stopRefreshInterval();
    window.removeEventListener('blur', this.handleBlur);
    window.removeEventListener('focus', this.handleFocus);
  }

  handleBlur() {
    this.stopRefreshInterval();
  }

  handleFocus() {
    refresh(this.props);
    this.startRefreshInterval();
  }

  startRefreshInterval() {
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
