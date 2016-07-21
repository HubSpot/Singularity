export const mergeSorted = (a, b) => {
  const answer = new Array(a.length + b.length);
  let i = 0;
  let j = 0;
  let k = 0;

  while (i < a.length && j < b.length) {
    if (a[i].name < b[j].name) {
      answer[k] = a[i];
      i++;
    } else {
      answer[k] = b[j];
      j++;
    }
    k++;
  }
  while (i < a.length) {
    answer[k] = a[i];
    i++;
    k++;
  }
  while (j < b.length) {
    answer[k] = b[j];
    j++;
    k++;
  }
  return answer;
};

// Usage:
// const a = [{name:"a"}, {name:"b"}, {name:"m"}, {name:"x"}];
// const b = [{name:"a"}, {name:"e"}, {name:"i"}, {name:"o"}];
// const c = [{name:"g"}, {name:"h"}, {name:"m"}, {name:"n"}];
// mergeAll(a,b,c).map(function(x){return x.name;});
export const mergeAll = () => {
  return Array.prototype.slice.call(arguments).reduce(mergeSorted);
};
