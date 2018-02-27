import React, { Component, PropTypes } from 'react';
import {connect} from 'react-redux';
import Messenger from 'messenger';
import Navigation from './Navigation';
import GlobalSearch from '../globalSearch/GlobalSearch';
import Title from './Title';
import Utils from '../../utils';

const PAUSE_INTERVAL_IN_MS = 1000 * 60 * 60;
const LAG_THRESHOLD_IN_MS = 1000 * 60 * 3;

class Application extends Component {
  constructor(props) {
    super(props);
    this.state = {
      canShowLagError: true,
    };
    _.bindAll(this,
      'pauseNotification',
      'notifyLag',
    );
  }

  componentWillUnmount() {
    clearInterval(this.timerId);
  }

  pauseNotification() {
    this.setState({ canShowLagError: false });
    this.timerId = setInterval(() => {
      clearInterval(this.timerId);
      this.setState({ canShowLagError: true });
    }, PAUSE_INTERVAL_IN_MS);
  }

  notifyLag(maxTaskLag) {
    const { canShowLagError: canNotify } = this.state;
    const shouldNotify = maxTaskLag >= LAG_THRESHOLD_IN_MS;
    if (canNotify && shouldNotify) {
      Messenger().error({
        onClickClose: this.pauseNotification,
        message: `
          Singularity is experiencing some delays. The team has already been
          notified. (Max task lag: ${Utils.duration(maxTaskLag)})
        `,
      });
    }
  }

  render() {
    this.notifyLag(this.props.maxTaskLag);

    return (
      <div>
        <Title routes={this.props.routes} params={this.props.params} />
        <Navigation location={this.props.location} history={this.props.history} />
        <GlobalSearch />
        {this.props.children}
      </div>
    );
  }
}

Application.propTypes = {
  children: PropTypes.object,
  history: PropTypes.object.isRequired,
  location: PropTypes.object.isRequired,
  maxTaskLag: PropTypes.number,
  params: PropTypes.object.isRequired,
  routes: PropTypes.arrayOf(PropTypes.object).isRequired,
};

const mapStateToProps = (state) => {
  return {
    maxTaskLag: state.api.status.data.maxTaskLag
  };
};

export default connect(mapStateToProps)(Application);
