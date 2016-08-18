import React from 'react';
import classNames from 'classnames';

import { connect } from 'react-redux';
import { selectLogColor } from '../../actions/log';

class ColorDropdown extends React.Component {
  renderColorChoices() {
    let { activeColor } = this.props;

    return this.props.colors.map((color, index) => {
      const colorClass = color.toLowerCase().replace(' ', '-');
      const className = classNames({ active: activeColor === colorClass });
      return React.createElement('li', { 'key': index, className }, React.createElement('a', { ['onClick']: () => this.props.selectLogColor(colorClass) }, color));
    });
  }

  render() {
    return (
      <div className="btn-group" title="Select Color Scheme">
        <button type="button" className="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
          <span className="glyphicon glyphicon-adjust" /> <span className="caret" />
        </button>
        <ul className="dropdown-menu dropdown-menu-right">
          {this.renderColorChoices()}
        </ul>
      </div>
    );
  }
}

let mapStateToProps = state => ({
  colors: state.colors,
  activeColor: state.activeColor
});

let mapDispatchToProps = { selectLogColor };

export default connect(mapStateToProps, mapDispatchToProps)(ColorDropdown);
