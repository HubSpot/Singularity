import React from 'react';

export default class CollapsableSection extends React.Component {

  constructor(props) {
    super();
    this.state = {
      expanded: props.defaultExpanded
    }
  }

  toggle() {
    this.setState({
      expanded: !this.state.expanded
    });
  }

  render() {
    return (
      <div id={this.props.id}>
        <div className="page-header">
            <h2>
              {this.props.title}
              <small>
                  <a data-action="expandToggle" onClick={this.toggle.bind(this)}>{this.state.expanded ? 'Collapse' : 'View'}</a>
              </small>
            </h2>
        </div>
        {this.state.expanded ? this.props.children : <div></div>}
      </div>
    );
  }
}
