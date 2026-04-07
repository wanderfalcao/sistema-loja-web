package br.com.infnet.pedido.mapper;

import br.com.infnet.pedido.domain.Pedido;
import br.com.infnet.pedido.dto.PedidoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PedidoMapper {

    @Mapping(target = "valor",      expression = "java(pedido.getValor().quantia())")
    @Mapping(target = "valorTotal", expression = "java(pedido.calcularTotal())")
    PedidoResponse toResponse(Pedido pedido);
}
