import React, { PropTypes, Component } from 'react';
import classNames from 'classnames';
import NotFound from 'components/common/NotFound';

const rootComponent = (Wrapped, title, refresh = _.noop, refreshInterval = true, pageMargin = true) => class extends Component {

  static propTypes = {
    notFound: PropTypes.bool,
    pathname: PropTypes.string
  }

  constructor(props) {
    super(props);
    _.bindAll(this, 'handleBlur', 'handleFocus');

    /*
     NOTE: I tried moving this state into redux but it resulted in page transitions being signifcantly slower.
     Maybe revisit this in the future. (see branch rootcomponent_redux for implementation)
    */
    this.state = {
      loading: refresh !== _.noop
    };
  }

  componentWillMount() {
    const titleString = typeof title === 'function' ? title(this.props) : title;
    document.title = `${titleString} - ${config.title}`;

    const promise = refresh(this.props);
    if (promise) {
      promise.then(() => {
        if (!this.unmounted) {
          this.setState({
            loading: false
          });
        }
      });
    } else {
      this.setState({
        loading: false
      });
    }

    if (refreshInterval) {
      this.startRefreshInterval();
      window.addEventListener('blur', this.handleBlur);
      window.addEventListener('focus', this.handleFocus);
    }
  }

  componentWillUnmount() {
    this.unmounted = true;
    if (refreshInterval) {
      this.stopRefreshInterval();
      window.removeEventListener('blur', this.handleBlur);
      window.removeEventListener('focus', this.handleFocus);
    }
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
    if (this.props.notFound) {
      return (
        <div className={classNames({'page container-fluid': pageMargin})}>
          <NotFound location={{pathname: this.props.pathname}} />
        </div>
      );
    }
    const loader = this.state.loading && <div className="page-loader fixed" />;
    const page = !this.state.loading && <Wrapped {...this.props} />;
    return (
      <div className={classNames({'page container-fluid': pageMargin})}>
        {loader}
        {page}
      </div>
    );
  }
};

export default rootComponent;
