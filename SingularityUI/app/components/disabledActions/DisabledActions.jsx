import React, { PropTypes } from 'react';
import Utils from '../../utils';
import { FetchDisabledActions } from '../../actions/api/disabledActions';
import { connect } from 'react-redux';
import Column from '../common/table/Column';
import UITable from '../common/table/UITable';
import rootComponent from '../../rootComponent';
import DeleteDisabledActionButton from '../common/modalButtons/DeleteDisabledActionButton';
import NewDisabledActionButton from '../common/modalButtons/NewDisabledActionButton';

const DisabledActions = ({disabledActions, user}) => (
    <div>
        <div className="row">
          <div className="col-md-10 col-xs-6">
            <span className="h1">Disabled Actions</span>
          </div>
          <div className="col-md-2 col-xs-6 button-container">
            <NewDisabledActionButton user={user}>
              <button
                className="btn btn-warning pull-right"
                alt="Disable an Action"
                title="newDisabledAction">
                New Disabled Action
              </button>
            </NewDisabledActionButton>
          </div>
        </div>
        <UITable
          emptyTableMessage="No Actions Are Disabled"
          data={disabledActions}
          keyGetter={(disabledAction) => disabledAction.type}
          defaultSortBy="type"
          defaultSortDirection={UITable.SortDirection.ASC}
        >
            <Column
                label="Type"
                id="type"
                key="type"
                sortable={true}
                sortData={(cellData, disabledAction) => disabledAction.type}
                cellData={(disabledAction) => Utils.humanizeText(disabledAction.type)}
            />
            <Column
                label="Message"
                id="message"
                key="message"
                cellData={(disabledAction) => disabledAction.message}
            />
            <Column
                label="User"
                id="user"
                key="user"
                cellData={(disabledAction) => disabledAction.user}
            />
            <Column
                id="actions-column"
                key="actions-column"
                className="actions-column"
                cellData={(disabledAction) => <DeleteDisabledActionButton disabledAction={disabledAction} />}
            />
        </UITable>
    </div>
);

DisabledActions.propTypes = {
  disabledActions: PropTypes.arrayOf(PropTypes.shape({
      type: PropTypes.string.isRequired,
      message: PropTypes.string,
      user: PropTypes.string
  })).isRequired,
  user: PropTypes.string,
  fetchDisabledActions: PropTypes.func.isRequired
};

function mapStateToProps(state) {
  const user = Utils.maybe(state, ['api', 'user', 'data', 'user', 'name']);
  return {
    user,
    disabledActions: state.api.disabledActions.data
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchDisabledActions: () => dispatch(FetchDisabledActions.trigger())
  };
}

function refresh(props) {
  return props.fetchDisabledActions();
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(DisabledActions, 'Disabled Actions', refresh));
