import React, {PropTypes} from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';

const GroupDetail = ({group}) => {
  if (!group) return <div className="loader loader-fixed"></div>;
  return (
    <h2>{group.id}</h2>
  );
};

function mapStateToProps(state, ownProps) {
  const group = _.find(state.api.requestGroups.data, (g) => g.id === ownProps.params.groupId);
  return ({
    notFound: !state.api.requestGroups.isFetching && !group,
    pathname: ownProps.location.pathname,
    group
  });
}

GroupDetail.propTypes = {
  group: PropTypes.object
};

export default connect(mapStateToProps)(rootComponent(GroupDetail, (props) => `Group ${props.params.groupId}`));
