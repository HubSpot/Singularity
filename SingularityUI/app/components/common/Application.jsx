import React, { Component, PropTypes } from 'react';
import {connect} from 'react-redux';
import Messenger from 'messenger';
import Navigation from './Navigation';
import GlobalSearch from '../globalSearch/GlobalSearch';
import Title from './Title';
import Utils from '../../utils';

class Application extends Component {
  constructor(props) {
    super(props);
    this.state = {
      lagErrorDismissed: false,
    };
    this.dismissLagError = this.dismissLagError.bind(this);
    this.notifyLag = this.notifyLag.bind(this);
  }

  dismissLagError() {
    this.setState({ lagErrorDismissed: true });
  }

  notifyLag(maxTaskLag) {
    const { lagErrorDismissed } = this.state;
    const taskLagMinutes = maxTaskLag / 1000 / 60;
    if (!lagErrorDismissed && taskLagMinutes >= 0) {
      Messenger().error({
        onClickClose: this.dismissLagError,
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
