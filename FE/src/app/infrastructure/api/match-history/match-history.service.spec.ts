import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatchHistoryService } from './match-history.service';

describe('MatchHistoryService', () => {
  let service: MatchHistoryService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(MatchHistoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads authenticated player stats', () => {
    let totalMatches = 0;

    service.getPlayerStats().subscribe((stats) => {
      totalMatches = stats.totalMatches;
    });

    const request = httpMock.expectOne('http://localhost:8080/api/matches/stats');
    expect(request.request.method).toBe('GET');
    request.flush({ totalMatches: 4, wins: 3, losses: 1, winRate: 75, currentStreak: 2 });

    expect(totalMatches).toBe(4);
  });

  it('loads paginated history with filter params', () => {
    let responseSize = 0;

    service.getHistory('WINS', 1, 5).subscribe((response) => {
      responseSize = response.items.length;
    });

    const request = httpMock.expectOne((candidate) => candidate.url === 'http://localhost:8080/api/matches/history');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('filter')).toBe('WINS');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('5');
    request.flush({
      items: [
        {
          matchId: 'match-1',
          result: 'VICTORIA',
          opponentName: 'Misty',
          date: '2026-06-01T10:15:30Z',
          turnsPlayed: 9
        }
      ],
      page: 1,
      size: 5,
      totalItems: 1,
      totalPages: 1,
      first: true,
      last: true
    });

    expect(responseSize).toBe(1);
  });
});
