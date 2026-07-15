package ar.edu.utn.frc.tup.piii.services.card;

import ar.edu.utn.frc.tup.piii.dtos.card.CardImportResultDto;
import ar.edu.utn.frc.tup.piii.dtos.card.CardImportStatusDto;

public interface CardImportService {

    CardImportResultDto importXy1();

    CardImportStatusDto getXy1Status();
}
