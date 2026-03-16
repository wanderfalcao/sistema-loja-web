document.getElementById('modalContestar').addEventListener('show.bs.modal', function (e) {
    const pedidoId = e.relatedTarget.getAttribute('data-pedido-id');
    document.getElementById('formContestar').action = '/pedidos/' + pedidoId + '/contestar';
    document.getElementById('motivoContestacao').value = '';
});
