// Show the reexecute bar only when at least one request is selected, and keep a live count.
document.addEventListener('DOMContentLoaded', function () {
  var boxes = Array.prototype.slice.call(document.querySelectorAll('input[name="requestIds"]'));
  var bar = document.getElementById('reexecBar');
  var count = document.getElementById('selCount');
  var selectAll = document.getElementById('selectAll');

  function update() {
    var n = boxes.filter(function (b) { return b.checked; }).length;
    if (count) { count.textContent = n; }
    if (bar) { bar.style.display = n > 0 ? 'flex' : 'none'; }
  }

  boxes.forEach(function (b) { b.addEventListener('change', update); });

  if (selectAll) {
    selectAll.addEventListener('change', function () {
      boxes.forEach(function (b) { b.checked = selectAll.checked; });
      update();
    });
  }

  update();
});
