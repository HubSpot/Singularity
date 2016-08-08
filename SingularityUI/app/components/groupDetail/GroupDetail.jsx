import React, {PropTypes} from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';

class GroupDetail extends React.Component {

  static propTypes = {
    group: PropTypes.object
  }

  render() {
    if (!this.props.group) return <div className="loader loader-fixed"></div>;
    return (
      <h2>{this.props.group.id}</h2>
    );
  }
}

function mapStateToProps(state, ownProps) {
  return ({
    group: _.find(state.api.requestGroups.data, (group) => group.id === ownProps.params.groupId)
  });
}

export default connect(mapStateToProps)(rootComponent(GroupDetail, (props) => `Group ${props.params.groupId}`));
