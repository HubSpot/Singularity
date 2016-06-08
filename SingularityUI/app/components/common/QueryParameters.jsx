import React from 'react';
import QueryParameter from "./atomicDisplayItems/QueryParameter";

let QueryParameters = React.createClass({

    renderParameters() {
        return this.props.parameters.map((parameter, key) => {
            if (parameter.show) {
                return <div key={key}><QueryParameter paramName={parameter.name} paramValue={parameter.value} clearFn={parameter.clearFn} cantClear={parameter.cantClear} /></div>;
            }
        });
    },

    render() {
        return <div className="row"><div className={`col-${ this.props.colSize }`}><ul className="list-group">{this.renderParameters()}</ul></div></div>;
    }
});

export default QueryParameters;

