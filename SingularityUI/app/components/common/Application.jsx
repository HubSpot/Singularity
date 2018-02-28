import React, { Component, PropTypes } from 'react';
import {connect} from 'react-redux';
import Messenger from 'messenger';
import Navigation from './Navigation';
import GlobalSearch from '../globalSearch/GlobalSearch';
import Title from './Title';
import Utils from '../../utils';

const DISMISS_TASK_LAG_NOFICATION_DURATION_IN_MS = 1000 * 60 * 60;
const MAX_TASK_LAG_NOTIFICATION_THRESHOLD_IN_MS = 1000 * 60 * 3;

class Application extends Component {
  constructor(props) {
    super(props);
    this.state = {
      canShowTaskLagNotification: true,
    };
    _.bindAll(this,
      'dismissTaskLagNotification',
      'notifyLag',
    );
  }

  componentWillUnmount() {
    clearTimeout(this.reenableTaskLagNotificationTimeoutId);
  }

  dismissTaskLagNotification() {
    this.setState({ canShowTaskLagNotification: false });
    this.reenableTaskLagNotificationTimeoutId = setTimeout(() => {
      this.setState({ canShowTaskLagNotification: true });
    }, DISMISS_TASK_LAG_NOFICATION_DURATION_IN_MS);
  }

  notifyLag(maxTaskLag) {
    const { canShowTaskLagNotification: canNotify } = this.state;
    const shouldNotify = maxTaskLag >= MAX_TASK_LAG_NOTIFICATION_THRESHOLD_IN_MS;
    if (canNotify && shouldNotify) {
      Messenger().error({
        onClickClose: this.dismissTaskLagNotification,
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
