package br.com.infnet.pedido.mapper;

import br.com.infnet.pedido.domain.ItemPedido;
import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.dto.ItemPedidoResponse;
import br.com.infnet.pedido.dto.PedidoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PedidoMapper {

    @Mapping(target = "valor",      expression = "java(pedido.getValor().quantia())")
    @Mapping(target = "valorTotal", expression = "java(pedido.calcularTotal())")
    PedidoResponse toResponse(Pedido pedido);

    List<PedidoResponse> toResponseList(List<Pedido> pedidos);

    @Mapping(target = "precoUnitario", expression = "java(item.getPrecoUnitario().quantia())")
    @Mapping(target = "quantidade",    expression = "java(item.getQuantidade().inteiro())")
    @Mapping(target = "subtotal",      expression = "java(item.getSubtotal().quantia())")
    ItemPedidoResponse toItemResponse(ItemPedido item);
}
